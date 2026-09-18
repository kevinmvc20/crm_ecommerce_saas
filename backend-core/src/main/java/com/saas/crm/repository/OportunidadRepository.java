package com.saas.crm.repository;

import com.saas.crm.domain.entity.Oportunidad;
import com.saas.crm.domain.enums.EtapaOportunidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad {@link Oportunidad}.
 *
 * <p>Todas las consultas están acotadas por {@code tenantId} para garantizar
 * el aislamiento multi-tenant.</p>
 */
@Repository
public interface OportunidadRepository extends JpaRepository<Oportunidad, UUID> {

    /**
     * Recupera todas las oportunidades del tenant en orden cronológico inverso.
     *
     * @param tenantId UUID del tenant
     * @return lista de oportunidades del tenant, más recientes primero
     */
    List<Oportunidad> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /**
     * Recupera las oportunidades del tenant filtradas por etapa, más recientes primero.
     * Usado para cargar individualmente cada columna del Kanban.
     *
     * @param tenantId UUID del tenant
     * @param etapa    etapa del pipeline a filtrar
     * @return lista de oportunidades del tenant en la etapa indicada
     */
    List<Oportunidad> findByTenantIdAndEtapaOrderByCreatedAtDesc(
            UUID tenantId, EtapaOportunidad etapa);
}
