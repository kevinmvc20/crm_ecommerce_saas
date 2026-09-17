package com.saas.crm.repository;

import com.saas.crm.domain.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad {@link Cliente}.
 *
 * <p>Todas las consultas están acotadas por {@code tenantId} para garantizar
 * el aislamiento multi-tenant.</p>
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    /**
     * Recupera todos los clientes activos e inactivos del tenant indicado.
     *
     * @param tenantId UUID del tenant
     * @return lista de clientes del tenant
     */
    List<Cliente> findByTenantId(UUID tenantId);

    /**
     * Verifica si ya existe un cliente con el CI/NIT dado dentro del tenant.
     * Se usa para evitar duplicados en el proceso de conversión de leads.
     *
     * @param tenantId UUID del tenant
     * @param ciNit    número de CI o NIT a comprobar
     * @return {@code true} si el CI/NIT ya está registrado en ese tenant
     */
    boolean existsByTenantIdAndCiNit(UUID tenantId, String ciNit);
}
