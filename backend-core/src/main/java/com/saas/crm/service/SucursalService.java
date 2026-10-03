package com.saas.crm.service;

import com.saas.crm.domain.entity.Sucursal;
import com.saas.crm.domain.entity.Tenant;
import com.saas.crm.dto.inventario.SucursalCreateRequest;
import com.saas.crm.dto.inventario.SucursalResponse;
import com.saas.crm.repository.SucursalRepository;
import com.saas.crm.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de negocio para la gestión de sucursales del tenant.
 *
 * <p>Todas las operaciones están estrictamente acotadas al {@code tenantId}
 * extraído del principal de seguridad, garantizando el aislamiento multi-tenant.</p>
 */
@Service
@RequiredArgsConstructor
public class SucursalService {

    private final SucursalRepository sucursalRepository;
    private final TenantRepository tenantRepository;

    // ─── Listar Sucursales ────────────────────────────────────────────────────

    /**
     * Devuelve todas las sucursales del tenant, ordenadas por nombre.
     *
     * @param tenantId UUID del tenant autenticado
     * @return lista de {@link SucursalResponse}
     */
    @Transactional(readOnly = true)
    public List<SucursalResponse> listarSucursales(UUID tenantId) {
        return sucursalRepository.findByTenantIdOrderByNombreAsc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Registrar Sucursal ───────────────────────────────────────────────────

    /**
     * Registra una nueva sucursal para el tenant autenticado.
     *
     * @param tenantId UUID del tenant
     * @param request  payload con los datos de la sucursal
     * @return {@link SucursalResponse} con la sucursal persistida
     */
    @Transactional
    public SucursalResponse registrarSucursal(UUID tenantId, SucursalCreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant no encontrado."));

        Sucursal sucursal = new Sucursal();
        sucursal.setTenant(tenant);
        sucursal.setNombre(request.nombre());
        sucursal.setDireccion(request.direccion());
        sucursal.setTelefono(request.telefono());
        sucursal.setActivo(true);

        Sucursal saved = sucursalRepository.save(sucursal);
        return toResponse(saved);
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private SucursalResponse toResponse(Sucursal s) {
        return new SucursalResponse(
                s.getId(),
                s.getTenant().getId(),
                s.getNombre(),
                s.getDireccion(),
                s.getTelefono(),
                s.getActivo(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
