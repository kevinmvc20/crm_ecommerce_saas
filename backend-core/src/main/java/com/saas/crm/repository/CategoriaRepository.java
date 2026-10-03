package com.saas.crm.repository;

import com.saas.crm.domain.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para {@link Categoria}.
 *
 * <p>Todas las consultas están acotadas al {@code tenantId} del usuario
 * autenticado para garantizar el aislamiento multi-tenant.</p>
 */
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    /**
     * Lista únicamente las categorías raíz (sin padre) del tenant,
     * ordenadas por nombre.
     *
     * @param tenantId UUID del tenant propietario
     * @return lista de categorías raíz ordenada por nombre
     */
    List<Categoria> findByTenantIdAndCategoriaPadreIsNullOrderByNombreAsc(UUID tenantId);

    /**
     * Busca una categoría por su ID verificando que pertenezca al tenant.
     *
     * @param id       PK de la categoría
     * @param tenantId UUID del tenant
     * @return opcional con la categoría si existe y pertenece al tenant
     */
    Optional<Categoria> findByIdAndTenantId(Integer id, UUID tenantId);
}
