package com.saas.crm.dto.crm;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Payload de creación de un Lead.
 *
 * @param nombre     Nombre completo del prospecto (requerido).
 * @param email      Email de contacto (opcional).
 * @param telefono   Teléfono de contacto (opcional).
 * @param vendedorId UUID del vendedor a asignar (opcional).
 */
public record LeadCreateRequest(

        @NotBlank(message = "El nombre del lead es obligatorio.")
        String nombre,

        String email,

        String telefono,

        UUID vendedorId
) {}
