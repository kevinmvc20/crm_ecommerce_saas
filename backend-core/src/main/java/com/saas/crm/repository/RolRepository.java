package com.saas.crm.repository;

import com.saas.crm.domain.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link Rol}.
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {

    /**
     * Busca un rol global (sin tenant) por su nombre.
     *
     * @param nombre nombre del rol, ej. "ROLE_ADMIN_EMPRESA"
     * @return rol envuelto en Optional
     */
    Optional<Rol> findByNombre(String nombre);
}
