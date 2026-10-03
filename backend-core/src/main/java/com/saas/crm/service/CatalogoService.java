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
            if (vReq.sucursalIdInicial() != null
                    && vReq.stockInicial() != null
                    && vReq.stockInicial() > 0) {

                Sucursal sucursal = sucursalRepository
                        .findByIdAndTenantId(vReq.sucursalIdInicial(), tenantId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Sucursal '" + vReq.sucursalIdInicial()
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
        return new VarianteResponse(
                v.getId(),
                v.getProducto().getId(),
                v.getSku(),
                v.getNombreVariante(),
                v.getPrecio(),
                v.getActivo(),
                v.getCreatedAt(),
                v.getUpdatedAt()
        );
    }
}
