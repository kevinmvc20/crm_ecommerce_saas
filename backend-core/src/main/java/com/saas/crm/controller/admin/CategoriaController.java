package com.saas.crm.controller.admin;

import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.dto.inventario.CategoriaCreateRequest;
import com.saas.crm.dto.inventario.CategoriaResponse;
import com.saas.crm.service.CategoriaService;
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
 * Controlador REST para la gestión del árbol de categorías del catálogo.
 *
 * <p>Base URL: {@code /api/v1/admin/categorias}</p>
 * <p>Acceso restringido a {@code ROLE_ADMIN_EMPRESA}.</p>
 *
 * <p>El {@code tenantId} se extrae exclusivamente del {@link Usuario} autenticado
 * vía {@code @AuthenticationPrincipal}; nunca se recibe por JSON o query param.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/categorias")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
public class CategoriaController {

    private final CategoriaService categoriaService;

    // ─── GET / ────────────────────────────────────────────────────────────────

    /**
     * Devuelve el árbol de categorías del tenant autenticado.
     * Solo retorna las raíces; las subcategorías vienen anidadas en cada nodo.
     *
     * <p>GET {@code /api/v1/admin/categorias}</p>
     *
     * @param usuarioAuth usuario autenticado (principal de seguridad)
     * @return HTTP 200 con la lista de categorías raíz con sus subcategorías
     */
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listarArbolCategorias(
            @AuthenticationPrincipal Usuario usuarioAuth) {

        UUID tenantId = usuarioAuth.getTenant().getId();
        return ResponseEntity.ok(categoriaService.listarArbol(tenantId));
    }

    // ─── POST / ───────────────────────────────────────────────────────────────

    /**
     * Registra una nueva categoría (raíz o hija) para el tenant autenticado.
     *
     * <p>POST {@code /api/v1/admin/categorias}</p>
     *
     * @param usuarioAuth usuario autenticado
     * @param request     payload con nombre, descripción y padre opcional
     * @return HTTP 201 con la {@link CategoriaResponse} creada
     */
    @PostMapping
    public ResponseEntity<CategoriaResponse> registrarCategoria(
            @AuthenticationPrincipal Usuario usuarioAuth,
            @Valid @RequestBody CategoriaCreateRequest request) {

        UUID tenantId = usuarioAuth.getTenant().getId();
        CategoriaResponse created = categoriaService.registrarCategoria(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
