package com.saas.crm.controller.saas;

import com.saas.crm.dto.usuario.UsuarioCreateRequest;
import com.saas.crm.dto.usuario.UsuarioResponse;
import com.saas.crm.service.TenantUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para el aprovisionamiento y consulta de usuarios
 * operacionales (administradores y vendedores) dentro de un Tenant.
 *
 * <p>Base URL: {@code /api/v1/tenants/{tenantId}/usuarios}</p>
 * <p>Acceso restringido a {@code ROLE_SUPER_ADMIN} y {@code ROLE_ADMIN_EMPRESA}.</p>
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
public class TenantUsuarioController {

    private final TenantUsuarioService tenantUsuarioService;

    /**
     * POST /api/v1/tenants/{tenantId}/usuarios
     * <p>Aprovisiona un nuevo usuario operacional en el tenant indicado.</p>
     *
     * @param tenantId UUID del tenant destino (path variable)
     * @param request  payload validado con los datos del nuevo usuario
     * @return HTTP 201 con el {@link UsuarioResponse} del usuario creado
     */
    @PostMapping
    public ResponseEntity<UsuarioResponse> crearUsuario(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UsuarioCreateRequest request) {

        UsuarioResponse created = tenantUsuarioService.crearUsuarioParaTenant(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/v1/tenants/{tenantId}/usuarios
     * <p>Lista todos los usuarios operacionales del tenant indicado.</p>
     *
     * @param tenantId UUID del tenant (path variable)
     * @return HTTP 200 con la lista de {@link UsuarioResponse}
     */
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios(
            @PathVariable UUID tenantId) {

        List<UsuarioResponse> usuarios = tenantUsuarioService.listarUsuariosPorTenant(tenantId);
        return ResponseEntity.ok(usuarios);
    }
}
