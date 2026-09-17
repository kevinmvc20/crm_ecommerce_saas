package com.saas.crm.controller.saas;

import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.dto.usuario.UsuarioCreateRequest;
import com.saas.crm.dto.usuario.UsuarioResponse;
import com.saas.crm.service.TenantUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para el aprovisionamiento y consulta de usuarios
 * operacionales (administradores y vendedores) dentro de un Tenant.
 *
 * <p>
 * Base URL: {@code /api/v1/tenants/{tenantId}/usuarios}
 * </p>
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/usuarios")
@RequiredArgsConstructor
public class TenantUsuarioController {

    private final TenantUsuarioService tenantUsuarioService;

    /**
     * POST /api/v1/tenants/{tenantId}/usuarios
     * <p>
     * Aprovisiona un nuevo usuario operacional en el tenant indicado.
     * </p>
     * <p>
     * Acceso restringido exclusivamente a administradores.
     * </p>
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<UsuarioResponse> crearUsuario(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UsuarioCreateRequest request) {

        UsuarioResponse created = tenantUsuarioService.crearUsuarioParaTenant(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/v1/tenants/{tenantId}/usuarios
     * <p>
     * Lista los usuarios del tenant. Permite lectura a VENDEDOR con validación
     * anti-IDOR.
     * </p>
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'VENDEDOR')")
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal Usuario usuarioAuth) {

        // Validación Anti-IDOR: Si no es SUPER_ADMIN, debe pertenecer al mismo tenant
        // solicitado
        boolean isSuperAdmin = usuarioAuth.getRol() != null
                && "ROLE_SUPER_ADMIN".equals(usuarioAuth.getRol().getNombre());

        if (!isSuperAdmin) {
            if (usuarioAuth.getTenant() == null || !usuarioAuth.getTenant().getId().equals(tenantId)) {
                throw new AccessDeniedException("Acceso no autorizado al equipo de otra empresa.");
            }
        }

        List<UsuarioResponse> usuarios = tenantUsuarioService.listarUsuariosPorTenant(tenantId);
        return ResponseEntity.ok(usuarios);
    }
}