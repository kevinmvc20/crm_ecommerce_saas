package com.saas.crm.service;

import com.saas.crm.domain.entity.PlanSaaS;
import com.saas.crm.domain.entity.Tenant;
import com.saas.crm.dto.saas.TenantCreateRequest;
import com.saas.crm.dto.saas.TenantResponse;
import com.saas.crm.repository.PlanSaaSRepository;
import com.saas.crm.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Servicio de negocio para la gestión de Tenants (empresas inquilinas).
 */
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final PlanSaaSRepository planSaaSRepository;

    /**
     * Lista todos los tenants registrados en la plataforma mapeados a {@link TenantResponse}.
     *
     * @return lista completa de tenants
     */
    @Transactional(readOnly = true)
    public List<TenantResponse> listarTodos() {
        return tenantRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Crea un nuevo Tenant aplicando las reglas de negocio:
     * <ol>
     *   <li>Subdominio único (HTTP 409 si ya existe).</li>
     *   <li>Plan SaaS existente y activo (HTTP 404 / HTTP 400 según el caso).</li>
     *   <li>Persiste con {@code estadoSuscripcion = "ACTIVO"} y {@code activo = true}.</li>
     * </ol>
     *
     * @param request datos de creación del tenant
     * @return {@link TenantResponse} con la información del tenant creado
     */
    @Transactional
    public TenantResponse crearTenant(TenantCreateRequest request) {

        // 1. Validar subdominio único
        if (tenantRepository.existsBySubdominio(request.subdominio())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El subdominio '%s' ya está en uso. Elige otro.".formatted(request.subdominio())
            );
        }

        // 2. Validar plan existente y activo
        PlanSaaS plan = planSaaSRepository.findById(request.planSaaSId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un plan SaaS con id: " + request.planSaaSId()
                ));

        if (!Boolean.TRUE.equals(plan.getActivo())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El plan SaaS seleccionado no está activo."
            );
        }

        // 3. Construir y persistir la entidad
        Tenant tenant = new Tenant();
        tenant.setNombreComercial(request.nombreComercial());
        tenant.setSubdominio(request.subdominio());
        tenant.setPlanSaaS(plan);
        tenant.setEstadoSuscripcion("ACTIVO");
        tenant.setActivo(true);

        Tenant saved = tenantRepository.save(tenant);

        return toResponse(saved);
    }

    // ─── Mapeo interno ───────────────────────────────────────────────────────

    private TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getNombreComercial(),
                tenant.getSubdominio(),
                tenant.getEstadoSuscripcion(),
                tenant.getPlanSaaS().getNombre(),
                tenant.getActivo()
        );
    }
}
