package com.saas.crm.controller.admin;

import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.dto.inventario.ProductoCreateRequest;
import com.saas.crm.dto.inventario.ProductoResponse;
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
}
