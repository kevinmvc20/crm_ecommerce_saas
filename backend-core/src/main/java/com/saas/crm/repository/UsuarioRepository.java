package com.saas.crm.repository;

import com.saas.crm.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    /**
     * Recupera todos los usuarios asociados a un tenant específico.
     *
     * @param tenantId UUID del tenant
     * @return lista de usuarios del tenant
     */
    List<Usuario> findByTenantId(UUID tenantId);

    /**
     * Verifica si ya existe un usuario con el email indicado (scope global).
     *
     * @param email email a comprobar
     * @return {@code true} si el email ya está registrado
     */
    boolean existsByEmail(String email);

    /**
     * Cuenta los usuarios de un tenant excluyendo un rol específico.
     * Se usa para validar el límite del plan sin contar compradores finales.
     *
     * @param tenantId          UUID del tenant
     * @param rolNombreExcluido nombre del rol a excluir del conteo
     * @return número de usuarios operacionales actuales del tenant
     */
    long countByTenantIdAndRolNombreNot(UUID tenantId, String rolNombreExcluido);
}
