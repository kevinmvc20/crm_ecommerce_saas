package com.saas.crm.dto.usuario;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta inmutable con los datos públicos de un usuario operacional.
 *
 * @param id         identificador único del usuario
 * @param tenantId   UUID del tenant al que pertenece
 * @param email      dirección de correo electrónico
 * @param rolNombre  nombre del rol asignado
 * @param activo     indica si la cuenta está habilitada
 * @param createdAt  fecha y hora de creación del registro
 */
public record UsuarioResponse(
        UUID id,
        UUID tenantId,
        String email,
        String rolNombre,
        Boolean activo,
        LocalDateTime createdAt
) {}
