package com.saas.crm.controller.admin;

import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.dto.inventario.*;
import com.saas.crm.service.CatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión del catálogo de productos y sus variantes.
 *
 * <p>Base URL: {@code /api/v1/admin/productos}</p>
 * <p>Acceso restringido a {@code ROLE_ADMIN_EMPRESA}.</p>
 *
 * <p>El {@code tenantId} se extrae exclusivamente del {@link Usuario} autenticado
 * vía {@code @AuthenticationPrincipal}; nunca se recibe por JSON o query param.</p>
 *
 * <p>La creación aplica el <b>candado SaaS</b>: valida que el número de productos
 * activos del tenant no supere el {@code limiteProductos} de su plan.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/productos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
public class CatalogoController {

    private final CatalogoService catalogoService;

    // ─── GET / ────────────────────────────────────────────────────────────────

    /**
     * Lista todos los productos del tenant autenticado, del más reciente al más antiguo.
     *
     * <p>GET {@code /api/v1/admin/productos}</p>
     *
     * @param usuarioAuth usuario autenticado (principal de seguridad)
     * @return HTTP 200 con la lista de {@link ProductoResponse} y sus variantes
     */
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listarProductos(
            @AuthenticationPrincipal Usuario usuarioAuth) {

        UUID tenantId = usuarioAuth.getTenant().getId();
        return ResponseEntity.ok(catalogoService.listarProductos(tenantId));
    }

    // ─── POST / ───────────────────────────────────────────────────────────────

    /**
     * Registra un nuevo producto con sus variantes de compra.
     * Aplica validación de cuota SaaS y unicidad de SKU por tenant.
     *
     * <p>POST {@code /api/v1/admin/productos}</p>
     *
     * @param usuarioAuth usuario autenticado
     * @param request     payload con nombre, categoría y lista de variantes
     * @return HTTP 201 con el {@link ProductoResponse} creado (incluye variantes e ids)
     */
    @PostMapping
    public ResponseEntity<ProductoResponse> crearProducto(
            @AuthenticationPrincipal Usuario usuarioAuth,
            @Valid @RequestBody ProductoCreateRequest request) {

        UUID tenantId = usuarioAuth.getTenant().getId();
        ProductoResponse created = catalogoService.crearProducto(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── PUT /{id} ────────────────────────────────────────────────────────────

    /**
     * Actualiza los datos descriptivos de un producto existente.
     * Solo modifica nombre, descripción y categoría; no afecta variantes ni stock.
     *
     * <p>PUT {@code /api/v1/admin/productos/{id}}</p>
     *
     * @param usuarioAuth usuario autenticado
     * @param id          UUID del producto a actualizar
     * @param request     payload con los nuevos datos descriptivos
     * @return HTTP 200 con el {@link ProductoResponse} actualizado
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizarProducto(
            @AuthenticationPrincipal Usuario usuarioAuth,
            @PathVariable UUID id,
            @Valid @RequestBody ProductoUpdateRequest request) {

        UUID tenantId = usuarioAuth.getTenant().getId();
        ProductoResponse updated = catalogoService.actualizarProducto(tenantId, id, request);
        return ResponseEntity.ok(updated);
    }

    // ─── PATCH /{id}/toggle-activo ────────────────────────────────────────────

    /**
     * Alterna el estado activo/inactivo del producto (baja lógica).
     * No elimina variantes ni stock; preserva integridad referencial.
     *
     * <p>PATCH {@code /api/v1/admin/productos/{id}/toggle-activo}</p>
     *
     * @param usuarioAuth usuario autenticado
     * @param id          UUID del producto a alternar
     * @return HTTP 200 con el {@link ProductoResponse} y el nuevo estado {@code activo}
     */
    @PatchMapping("/{id}/toggle-activo")
    public ResponseEntity<ProductoResponse> toggleActivoProducto(
            @AuthenticationPrincipal Usuario usuarioAuth,
            @PathVariable UUID id) {

        UUID tenantId = usuarioAuth.getTenant().getId();
        ProductoResponse toggled = catalogoService.toggleActivoProducto(tenantId, id);
        return ResponseEntity.ok(toggled);
    }

    // ─── POST /{id}/variantes ─────────────────────────────────────────────────

    /**
     * Agrega una variante adicional a un producto existente sin duplicar el producto base.
     * Opcionalmente inicializa el stock en la sucursal indicada.
     *
     * <p>POST {@code /api/v1/admin/productos/{id}/variantes}</p>
     *
     * @param usuarioAuth usuario autenticado
     * @param id          UUID del producto padre
     * @param request     payload con los datos de la nueva variante
     * @return HTTP 201 con el {@link VarianteResponse} creado
     */
    @PostMapping("/{id}/variantes")
    public ResponseEntity<VarianteResponse> agregarVariante(
            @AuthenticationPrincipal Usuario usuarioAuth,
            @PathVariable UUID id,
            @Valid @RequestBody VarianteAdicionalRequest request) {

        UUID tenantId = usuarioAuth.getTenant().getId();
        VarianteResponse created = catalogoService.agregarVariante(tenantId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── POST /variantes/{varianteId}/stock/sucursales/{sucursalId} ───────────

    /**
     * Ajusta el stock físico y mínimo de una variante en una sucursal específica.
     * Si el registro no existe se crea automáticamente (upsert).
     * El stock físico no puede quedar por debajo del stock reservado.
     *
     * <p>POST {@code /api/v1/admin/productos/variantes/{varianteId}/stock/sucursales/{sucursalId}}</p>
     *
     * @param usuarioAuth usuario autenticado
     * @param varianteId  UUID de la variante de producto
     * @param sucursalId  ID de la sucursal
     * @param request     payload con el nuevo stock físico y mínimo
     * @return HTTP 200 con el {@link StockResponse} actualizado
     */
    @PostMapping("/variantes/{varianteId}/stock/sucursales/{sucursalId}")
    public ResponseEntity<StockResponse> ajustarStock(
            @AuthenticationPrincipal Usuario usuarioAuth,
            @PathVariable UUID varianteId,
            @PathVariable Integer sucursalId,
            @Valid @RequestBody AjusteStockRequest request) {

        UUID tenantId = usuarioAuth.getTenant().getId();
        StockResponse stock = catalogoService.ajustarStock(tenantId, varianteId, sucursalId, request);
        return ResponseEntity.ok(stock);
    }
}
