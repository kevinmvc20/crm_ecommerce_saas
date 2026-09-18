package com.saas.crm.dto.crm;

import java.util.UUID;

/**
 * Respuesta inmutable con la representación pública de un Cliente.
 * Usada para poblar el selector de clientes en el Kanban de Oportunidades.
 *
 * @param id                  Identificador único del cliente.
 * @param razonSocialONombre  Nombre o razón social del cliente.
 * @param ciNit               CI o NIT del cliente.
 * @param email               Email de contacto (puede ser null).
 */
public record ClienteResponse(
        UUID id,
        String razonSocialONombre,
        String ciNit,
        String email
) {}
