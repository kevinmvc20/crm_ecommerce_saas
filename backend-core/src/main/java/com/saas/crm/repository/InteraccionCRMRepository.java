package com.saas.crm.repository;

import com.saas.crm.domain.entity.InteraccionCRM;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad {@link InteraccionCRM}.
 *
 * <p>Todas las consultas están acotadas por {@code tenantId} para garantizar
 * el aislamiento multi-tenant.</p>
 */
@Repository
public interface InteraccionCRMRepository extends JpaRepository<InteraccionCRM, UUID> {

    /**
     * Recupera el historial de interacciones de un lead específico dentro
     * del tenant, ordenado de más reciente a más antiguo.
     *
     * @param tenantId UUID del tenant
     * @param leadId   UUID del lead
     * @return lista de interacciones ordenada cronológicamente descendente
     */
    List<InteraccionCRM> findByTenantIdAndLeadIdOrderByFechaHoraDesc(UUID tenantId, UUID leadId);
}
