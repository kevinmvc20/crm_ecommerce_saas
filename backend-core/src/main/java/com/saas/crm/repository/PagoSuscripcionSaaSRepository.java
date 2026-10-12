package com.saas.crm.repository;

import com.saas.crm.domain.entity.PagoSuscripcionSaaS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad {@link PagoSuscripcionSaaS}.
 */
@Repository
public interface PagoSuscripcionSaaSRepository extends JpaRepository<PagoSuscripcionSaaS, UUID> {

    /**
     * Recupera todos los pagos de suscripcion registrados para un tenant.
     */
    List<PagoSuscripcionSaaS> findByTenantId(UUID tenantId);
}