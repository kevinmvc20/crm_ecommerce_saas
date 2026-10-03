package com.saas.crm.repository;

import com.saas.crm.domain.entity.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para {@link Sucursal}.
 *
 * <p>Todas las consultas están acotadas al {@code tenantId} del usuario
 * autenticado para garantizar el aislamiento multi-tenant.</p>
 */
@Repository
public interface SucursalRepository extends JpaRepository<Sucursal, Integer> {

    /**
     * Lista todas las sucursales activas e inactivas del tenant,
     * ordenadas alfabéticamente por nombre.
     *
     * @param tenantId UUID del tenant propietario
     * @return lista de sucursales ordenada por nombre
     */
    List<Sucursal> findByTenantIdOrderByNombreAsc(UUID tenantId);

    /**
     * Busca una sucursal por su ID comprobando que pertenezca al tenant.
     *
     * @param id       PK de la sucursal
     * @param tenantId UUID del tenant
     * @return opcional con la sucursal si existe y pertenece al tenant
     */
    Optional<Sucursal> findByIdAndTenantId(Integer id, UUID tenantId);
}
