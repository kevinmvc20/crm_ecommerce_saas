package com.saas.crm.repository;

import com.saas.crm.domain.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad {@link Tenant}.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Verifica si ya existe un tenant con el subdominio indicado.
     *
     * @param subdominio subdominio a comprobar
     * @return {@code true} si el subdominio ya está en uso
     */
    boolean existsBySubdominio(String subdominio);

    /**
     * Busca un tenant por su subdominio único.
     *
     * @param subdominio subdominio de búsqueda
     * @return tenant envuelto en Optional
     */
    Optional<Tenant> findBySubdominio(String subdominio);
}
