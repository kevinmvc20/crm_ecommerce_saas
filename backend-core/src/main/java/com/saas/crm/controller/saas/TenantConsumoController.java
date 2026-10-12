package com.saas.crm.controller.saas;

import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.dto.saas.TenantConsumoResponse;
import com.saas.crm.service.TenantConsumoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST que expone las métricas de cuotas y consumo del tenant
 * autenticado, accesible únicamente por el administrador de la empresa.
 *
 * <p>Base URL: {@code /api/v1/admin/tenant}</p>
 *
 * <h3>Endpoints expuestos:</h3>
 * <ul>
 *   <li>{@code GET /api/v1/admin/tenant/consumo} → cuotas y consumo actual.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/tenant")
@RequiredArgsConstructor
public class TenantConsumoController {

    private final TenantConsumoService tenantConsumoService;

    /**
     * GET /api/v1/admin/tenant/consumo
     *
     * <p>Devuelve las métricas de consumo del tenant del usuario autenticado:
     * nombre del plan, estado de suscripción, días restantes de membresía y
     * contadores actuales vs. límites de usuarios, productos y sucursales.</p>
     *
     * <p>Restringido a {@code ROLE_ADMIN_EMPRESA}. El {@code tenantId} se extrae
     * directamente del principal JWT, garantizando que cada admin solo vea
     * sus propias métricas (aislamiento multi-tenant).</p>
     *
     * @param usuario principal autenticado inyectado por Spring Security
     * @return HTTP 200 con {@link TenantConsumoResponse}
     */
    @GetMapping("/consumo")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<TenantConsumoResponse> obtenerConsumo(
            @AuthenticationPrincipal Usuario usuario) {

        // El tenant puede ser null en usuarios SUPER_ADMIN; @PreAuthorize filtra esto,
        // pero añadimos la guarda defensiva por robustez.
        if (usuario.getTenant() == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }

        TenantConsumoResponse consumo =
                tenantConsumoService.obtenerConsumoTenant(usuario.getTenant().getId());

        return ResponseEntity.ok(consumo);
    }
}
