package com.saas.crm.controller.admin;

import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.dto.inventario.SucursalCreateRequest;
import com.saas.crm.dto.inventario.SucursalResponse;
import com.saas.crm.service.SucursalService;
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
 * Controlador REST para la gestión de sucursales del tenant autenticado.
 *
 * <p>Base URL: {@code /api/v1/admin/sucursales}</p>
 * <p>Acceso restringido a {@code ROLE_ADMIN_EMPRESA}.</p>
 *
 * <p>El {@code tenantId} se extrae exclusivamente del {@link Usuario} autenticado
 * vía {@code @AuthenticationPrincipal}; nunca se recibe por JSON o query param.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/sucursales")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
public class SucursalController {

    private final SucursalService sucursalService;

    // ─── GET / ────────────────────────────────────────────────────────────────

    /**
     * Lista todas las sucursales del tenant autenticado, ordenadas por nombre.
     *
     * <p>GET {@code /api/v1/admin/sucursales}</p>
     *
     * @param usuarioAuth usuario autenticado (principal de seguridad)
     * @return HTTP 200 con la lista de {@link SucursalResponse}
     */
    @GetMapping
    public ResponseEntity<List<SucursalResponse>> listarSucursales(
            @AuthenticationPrincipal Usuario usuarioAuth) {

        UUID tenantId = usuarioAuth.getTenant().getId();
        return ResponseEntity.ok(sucursalService.listarSucursales(tenantId));
    }

    // ─── POST / ───────────────────────────────────────────────────────────────

    /**
     * Registra una nueva sucursal para el tenant autenticado.
     *
     * <p>POST {@code /api/v1/admin/sucursales}</p>
     *
     * @param usuarioAuth usuario autenticado
     * @param request     payload con nombre, dirección y teléfono de la sucursal
     * @return HTTP 201 con la {@link SucursalResponse} creada
     */
    @PostMapping
    public ResponseEntity<SucursalResponse> registrarSucursal(
            @AuthenticationPrincipal Usuario usuarioAuth,
            @Valid @RequestBody SucursalCreateRequest request) {

        UUID tenantId = usuarioAuth.getTenant().getId();
        SucursalResponse created = sucursalService.registrarSucursal(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── PUT /{id} ────────────────────────────────────────────────────────────

    /**
     * Actualiza una sucursal existente.
     *
     * <p>PUT {@code /api/v1/admin/sucursales/{id}}</p>
     */
    @PutMapping("/{id}")
    public ResponseEntity<SucursalResponse> actualizarSucursal(
            @AuthenticationPrincipal Usuario usuarioAuth,
            @PathVariable Integer id,
            @Valid @RequestBody com.saas.crm.dto.inventario.SucursalUpdateRequest request) {

        UUID tenantId = usuarioAuth.getTenant().getId();
        return ResponseEntity.ok(sucursalService.actualizarSucursal(tenantId, id, request));
    }

    // ─── PATCH /{id}/toggle-activo ────────────────────────────────────────────

    /**
     * Alterna el estado activo/inactivo de una sucursal.
     *
     * <p>PATCH {@code /api/v1/admin/sucursales/{id}/toggle-activo}</p>
     */
    @PatchMapping("/{id}/toggle-activo")
    public ResponseEntity<SucursalResponse> toggleActivoSucursal(
            @AuthenticationPrincipal Usuario usuarioAuth,
            @PathVariable Integer id) {

        UUID tenantId = usuarioAuth.getTenant().getId();
        return ResponseEntity.ok(sucursalService.toggleActivoSucursal(tenantId, id));
    }
}
