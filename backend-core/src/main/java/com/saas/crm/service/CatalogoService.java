package com.saas.crm.service;

import com.saas.crm.domain.entity.*;
import com.saas.crm.dto.inventario.*;
import com.saas.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de negocio para la gestión del catálogo de productos y sus variantes.
 *
 * <h3>Reglas de negocio implementadas:</h3>
 * <ol>
 *   <li><b>Candado SaaS</b>: al crear un producto, valida que el conteo de productos
 *       activos del tenant no exceda {@code limiteProductos} de su plan.</li>
 *   <li><b>Unicidad de SKU</b>: ninguna variante puede usar un SKU ya registrado
 *       en el mismo tenant.</li>
 *   <li><b>Inicialización de Stock</b>: si una variante trae {@code stockInicial} &gt; 0
 *       y un {@code sucursalIdInicial}, se crea el registro {@link StockSucursal}
 *       correspondiente en la misma transacción.</li>
 *   <li><b>Coherencia de Stock</b>: al ajustar stock, el stock físico no puede ser
 *       menor al stock reservado existente.</li>
 *   <li><b>Baja Lógica</b>: el toggle de activo/inactivo no elimina datos ni rompe
 *       integridad referencial.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final ProductoRepository productoRepository;
    private final VarianteProductoRepository varianteProductoRepository;
    private final StockSucursalRepository stockSucursalRepository;
    private final CategoriaRepository categoriaRepository;
    private final SucursalRepository sucursalRepository;
    private final TenantRepository tenantRepository;

    // ─── Listar Productos ─────────────────────────────────────────────────────

    /**
     * Devuelve todos los productos del tenant, del más reciente al más antiguo.
     *
     * @param tenantId UUID del tenant autenticado
     * @return lista de {@link ProductoResponse} con sus variantes
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarProductos(UUID tenantId) {
        return productoRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::toProductoResponse)
                .toList();
    }

    // ─── Crear Producto ───────────────────────────────────────────────────────

    /**
     * Crea un producto con sus variantes y, opcionalmente, inicializa el stock
     * de cada variante en la sucursal indicada.
     *
     * <p>La operación es completamente atómica: cualquier fallo revierte todo.</p>
     *
     * @param tenantId UUID del tenant autenticado
     * @param request  payload con nombre, descripción, categoría y variantes
     * @return {@link ProductoResponse} con el producto y variantes persistidos
     * @throws ResponseStatusException 400 si se supera el límite de productos del plan
     * @throws ResponseStatusException 409 si algún SKU ya existe en el tenant
     */
    @Transactional
    public ProductoResponse crearProducto(UUID tenantId, ProductoCreateRequest request) {

        // 1. Resolver Tenant
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant no encontrado."));

        // 2. Candado SaaS — validar cuota de productos
        if (tenant.getPlanSaaS() != null) {
            long activos = productoRepository.countByTenantIdAndActivoTrue(tenantId);
            if (activos >= tenant.getPlanSaaS().getLimiteProductos()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Límite de productos alcanzado en su plan. Actual: " + activos
                        + " / Límite: " + tenant.getPlanSaaS().getLimiteProductos() + ".");
            }
        }

        // 3. Validar unicidad de SKUs dentro del tenant
        for (VarianteCreateRequest v : request.variantes()) {
            varianteProductoRepository.findBySkuAndTenantId(v.sku(), tenantId).ifPresent(existing -> {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El SKU '" + v.sku() + "' ya está registrado en este tenant.");
            });
        }

        // 4. Resolver Categoría (opcional)
        Categoria categoria = null;
        if (request.categoriaId() != null) {
            categoria = categoriaRepository.findByIdAndTenantId(request.categoriaId(), tenantId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Categoría no encontrada o no pertenece al tenant."));
        }

        // 5. Crear Producto
        Producto producto = new Producto();
        producto.setTenant(tenant);
        producto.setCategoria(categoria);
        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setActivo(true);
        Producto productoSaved = productoRepository.save(producto);

        // 6. Crear Variantes e inicializar Stock si corresponde
        for (VarianteCreateRequest vReq : request.variantes()) {
            VarianteProducto variante = new VarianteProducto();
            variante.setTenant(tenant);
            variante.setProducto(productoSaved);
            variante.setSku(vReq.sku());
            variante.setNombreVariante(vReq.nombreVariante());
            variante.setPrecio(vReq.precio());
            variante.setActivo(true);
            VarianteProducto varianteSaved = varianteProductoRepository.save(variante);

            // Inicializar stock si se especificó una sucursal y stock > 0
            if (vReq.getSucursalEfectiva() != null
                    && vReq.stockInicial() != null
                    && vReq.stockInicial() > 0) {

                Sucursal sucursal = sucursalRepository
                        .findByIdAndTenantId(vReq.getSucursalEfectiva(), tenantId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Sucursal '" + vReq.getSucursalEfectiva()
                                + "' no encontrada o no pertenece al tenant."));

                StockSucursal stock = new StockSucursal();
                stock.setTenant(tenant);
                stock.setSucursal(sucursal);
                stock.setVarianteProducto(varianteSaved);
                stock.setStockFisico(vReq.stockInicial());
                stock.setStockReservado(0);
                stock.setStockMinimo(5);
                stockSucursalRepository.save(stock);
            }
        }

        // 7. Recargar producto con variantes para la respuesta
        Producto productoFinal = productoRepository.findByIdAndTenantId(productoSaved.getId(), tenantId)
                .orElse(productoSaved);

        return toProductoResponse(productoFinal);
    }

    // ─── Actualizar Producto ──────────────────────────────────────────────────

    /**
     * Actualiza los datos descriptivos de un producto existente (nombre,
     * descripción y categoría). No modifica variantes ni stock.
     *
     * @param tenantId   UUID del tenant autenticado
     * @param productoId UUID del producto a actualizar
     * @param request    payload con los nuevos datos descriptivos
     * @return {@link ProductoResponse} actualizado con sus variantes
     * @throws ResponseStatusException 404 si el producto no existe en el tenant
     * @throws ResponseStatusException 404 si la categoría no existe en el tenant
     */
    @Transactional
    public ProductoResponse actualizarProducto(UUID tenantId, UUID productoId, ProductoUpdateRequest request) {

        Producto producto = productoRepository.findByIdAndTenantId(productoId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Producto no encontrado o no pertenece al tenant."));

        // Resolver nueva categoría (null = desasociar)
        Categoria categoria = null;
        if (request.categoriaId() != null) {
            categoria = categoriaRepository.findByIdAndTenantId(request.categoriaId(), tenantId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Categoría no encontrada o no pertenece al tenant."));
        }

        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setCategoria(categoria);

        Producto updated = productoRepository.save(producto);
        return toProductoResponse(updated);
    }

    // ─── Toggle Activo / Inactivo ─────────────────────────────────────────────

    /**
     * Alterna el estado activo/inactivo de un producto (baja lógica).
     * No elimina datos ni variantes, preservando la integridad referencial.
     *
     * @param tenantId   UUID del tenant autenticado
     * @param productoId UUID del producto a alternar
     * @return {@link ProductoResponse} con el nuevo estado
     * @throws ResponseStatusException 404 si el producto no existe en el tenant
     */
    @Transactional
    public ProductoResponse toggleActivoProducto(UUID tenantId, UUID productoId) {

        Producto producto = productoRepository.findByIdAndTenantId(productoId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Producto no encontrado o no pertenece al tenant."));

        producto.setActivo(!producto.getActivo());
        Producto updated = productoRepository.save(producto);
        return toProductoResponse(updated);
    }

    // ─── Agregar Variante Adicional ───────────────────────────────────────────

    /**
     * Agrega una nueva variante a un producto existente, evitando duplicar
     * el producto base. Opcionalmente crea el registro de stock inicial.
     *
     * @param tenantId   UUID del tenant autenticado
     * @param productoId UUID del producto padre
     * @param req        payload con los datos de la nueva variante
     * @return {@link VarianteResponse} de la variante persistida
     * @throws ResponseStatusException 404 si el producto no existe en el tenant
     * @throws ResponseStatusException 409 si el SKU ya está registrado en el tenant
     * @throws ResponseStatusException 404 si la sucursal no existe en el tenant
     */
    @Transactional
    public VarianteResponse agregarVariante(UUID tenantId, UUID productoId, VarianteAdicionalRequest req) {

        // 1. Verificar que el producto existe en el tenant
        Producto producto = productoRepository.findByIdAndTenantId(productoId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Producto no encontrado o no pertenece al tenant."));

        // 2. Validar unicidad de SKU en el tenant
        varianteProductoRepository.findBySkuAndTenantId(req.sku(), tenantId).ifPresent(existing -> {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El SKU '" + req.sku() + "' ya está registrado en este tenant.");
        });

        // 3. Resolver Tenant
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant no encontrado."));

        // 4. Crear la variante
        VarianteProducto variante = new VarianteProducto();
        variante.setTenant(tenant);
        variante.setProducto(producto);
        variante.setSku(req.sku());
        variante.setNombreVariante(req.nombreVariante());
        variante.setPrecio(req.precio());
        variante.setActivo(true);
        VarianteProducto varianteSaved = varianteProductoRepository.save(variante);

        // 5. Inicializar stock si se especificó
        if (req.stockInicial() != null && req.stockInicial() > 0 && req.sucursalId() != null) {
            Sucursal sucursal = sucursalRepository.findByIdAndTenantId(req.sucursalId(), tenantId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Sucursal '" + req.sucursalId() + "' no encontrada o no pertenece al tenant."));

            StockSucursal stock = new StockSucursal();
            stock.setTenant(tenant);
            stock.setSucursal(sucursal);
            stock.setVarianteProducto(varianteSaved);
            stock.setStockFisico(req.stockInicial());
            stock.setStockReservado(0);
            stock.setStockMinimo(5);
            stockSucursalRepository.save(stock);
        }

        return toVarianteResponse(varianteSaved);
    }

    // ─── Ajustar Stock ────────────────────────────────────────────────────────

    /**
     * Ajusta el stock físico y el stock mínimo de una variante en una sucursal
     * concreta. Si el registro de stock no existe y {@code nuevoStockFisico >= 0},
     * lo crea automáticamente (upsert).
     *
     * <p>Regla de coherencia: el stock físico no puede quedar por debajo del
     * stock reservado existente ({@code chk_stock_coherencia}).</p>
     *
     * @param tenantId   UUID del tenant autenticado
     * @param varianteId UUID de la variante de producto
     * @param sucursalId ID de la sucursal
     * @param req        payload con el nuevo stock físico y mínimo
     * @return {@link StockResponse} con el estado actualizado
     * @throws ResponseStatusException 404 si la variante o la sucursal no pertenecen al tenant
     * @throws ResponseStatusException 400 si el nuevo stock físico sería menor al reservado
     */
    @Transactional
    public StockResponse ajustarStock(UUID tenantId, UUID varianteId, Integer sucursalId, AjusteStockRequest req) {

        // 1. Verificar que la variante existe en el tenant
        VarianteProducto variante = varianteProductoRepository.findById(varianteId)
                .filter(v -> v.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Variante no encontrada o no pertenece al tenant."));

        // 2. Verificar que la sucursal existe en el tenant
        Sucursal sucursal = sucursalRepository.findByIdAndTenantId(sucursalId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sucursal no encontrada o no pertenece al tenant."));

        // 3. Resolver Tenant
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant no encontrado."));

        // 4. Buscar o crear el registro de stock (upsert)
        StockSucursal stockSucursal = stockSucursalRepository
                .findBySucursalIdAndVarianteProductoIdAndTenantId(sucursalId, varianteId, tenantId)
                .orElse(null);

        if (stockSucursal == null) {
            // No existe aún: crear un nuevo registro
            stockSucursal = new StockSucursal();
            stockSucursal.setTenant(tenant);
            stockSucursal.setSucursal(sucursal);
            stockSucursal.setVarianteProducto(variante);
            stockSucursal.setStockReservado(0);
            stockSucursal.setStockMinimo(req.stockMinimo());
        } else {
            // Existe: validar coherencia (chk_stock_coherencia)
            if (req.nuevoStockFisico() < stockSucursal.getStockReservado()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El stock físico (" + req.nuevoStockFisico()
                        + ") no puede ser menor al stock reservado ("
                        + stockSucursal.getStockReservado() + ").");
            }
        }

        stockSucursal.setStockFisico(req.nuevoStockFisico());
        stockSucursal.setStockMinimo(req.stockMinimo());
        StockSucursal saved = stockSucursalRepository.save(stockSucursal);

        return toStockResponse(saved);
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private ProductoResponse toProductoResponse(Producto p) {
        List<VarianteResponse> variantes = varianteProductoRepository
                .findByProductoIdAndTenantId(p.getId(), p.getTenant().getId())
                .stream()
                .map(this::toVarianteResponse)
                .toList();

        return new ProductoResponse(
                p.getId(),
                p.getTenant().getId(),
                p.getCategoria() != null ? p.getCategoria().getId() : null,
                p.getNombre(),
                p.getDescripcion(),
                p.getActivo(),
                variantes,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    private VarianteResponse toVarianteResponse(VarianteProducto v) {
        List<StockResponse> stocks = stockSucursalRepository
                .findByVarianteProductoIdAndTenantId(v.getId(), v.getTenant().getId())
                .stream()
                .map(this::toStockResponse)
                .toList();

        return new VarianteResponse(
                v.getId(),
                v.getProducto().getId(),
                v.getSku(),
                v.getNombreVariante(),
                v.getPrecio(),
                v.getActivo(),
                stocks,
                v.getCreatedAt(),
                v.getUpdatedAt()
        );
    }

    private StockResponse toStockResponse(StockSucursal s) {
        return new StockResponse(
                s.getId(),
                s.getSucursal().getId(),
                s.getVarianteProducto().getId(),
                s.getStockFisico(),
                s.getStockReservado(),
                s.getStockDisponible(),
                s.getStockMinimo(),
                s.getUpdatedAt()
        );
    }
}
