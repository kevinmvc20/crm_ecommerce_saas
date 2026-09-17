package com.saas.crm.repository;

import com.saas.crm.domain.entity.Lead;
import com.saas.crm.domain.enums.EstadoLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad {@link Lead}.
 *
 * <p>Todas las consultas están acotadas por {@code tenantId} para garantizar
 * el aislamiento multi-tenant.</p>
 */
@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    /**
     * Recupera todos los leads del tenant indicado.
     *
     * @param tenantId UUID del tenant
     * @return lista de leads del tenant
     */
    List<Lead> findByTenantId(UUID tenantId);

    /**
     * Recupera los leads asignados a un vendedor específico dentro del tenant.
     *
     * @param tenantId   UUID del tenant
     * @param vendedorId UUID del vendedor asignado
     * @return lista de leads del vendedor en ese tenant
     */
    List<Lead> findByTenantIdAndVendedorAsignadoId(UUID tenantId, UUID vendedorId);

    /**
     * Cuenta los leads de un tenant en un estado determinado.
     * Útil para dashboards y métricas del pipeline.
     *
     * @param tenantId UUID del tenant
     * @param estado   estado del lead a filtrar
     * @return número de leads en ese estado
     */
    long countByTenantIdAndEstado(UUID tenantId, EstadoLead estado);
}
