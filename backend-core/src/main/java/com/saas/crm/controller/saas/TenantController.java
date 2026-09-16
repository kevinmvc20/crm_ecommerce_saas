package com.saas.crm.controller.saas;

import com.saas.crm.dto.saas.TenantCreateRequest;
import com.saas.crm.dto.saas.TenantResponse;
import com.saas.crm.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de Tenants (empresas inquilinas).
 * Base URL: {@code /api/v1/tenants}
 * <p>Todos los endpoints están restringidos al rol {@code ROLE_SUPER_ADMIN}.</p>
 */
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TenantController {

    private final TenantService tenantService;

    /**
     * GET /api/v1/tenants
     * <p>Lista todas las empresas registradas en la plataforma.</p>
     *
     * @return HTTP 200 con lista de {@link TenantResponse}
     */
    @GetMapping
    public ResponseEntity<List<TenantResponse>> listarTenants() {
        return ResponseEntity.ok(tenantService.listarTodos());
    }

    /**
     * POST /api/v1/tenants
     * <p>Da de alta una nueva empresa asignándole subdominio único y plan SaaS.</p>
     *
     * @param request payload validado con los datos del nuevo tenant
     * @return HTTP 201 con el {@link TenantResponse} del tenant creado
     */
    @PostMapping
    public ResponseEntity<TenantResponse> crearTenant(@Valid @RequestBody TenantCreateRequest request) {
        TenantResponse created = tenantService.crearTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
