package com.saas.crm.repository;

import com.saas.crm.domain.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para {@link Producto}.
 *
 * <p>Incluye el conteo de productos activos utilizado por el candado SaaS
 * para validar la cuota del plan.</p>
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, UUID> {

    /**
     * Lista todos los productos del tenant, ordenados del más reciente al más antiguo.
     *
     * @param tenantId UUID del tenant
     * @return lista de productos ordenada por {@code createdAt} descendente
     */
    List<Producto> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /**
     * Cuenta los productos <em>activos</em> del tenant.
     * Utilizado para validar la cuota {@code limiteProductos} del plan SaaS.
     *
     * @param tenantId UUID del tenant
     * @return número de productos activos
     */
    long countByTenantIdAndActivoTrue(UUID tenantId);

    /**
     * Busca un producto por su UUID verificando que pertenezca al tenant.
     *
     * @param id       UUID del producto
     * @param tenantId UUID del tenant
     * @return opcional con el producto si existe y pertenece al tenant
     */
    Optional<Producto> findByIdAndTenantId(UUID id, UUID tenantId);
}
