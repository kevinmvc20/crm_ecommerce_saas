package com.saas.crm.dto.crm;

import com.saas.crm.domain.enums.EstadoLead;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta inmutable con la representación pública de un Lead.
 *
 * @param id              Identificador único del lead.
 * @param tenantId        Tenant al que pertenece.
 * @param vendedorId      UUID del vendedor asignado (puede ser null).
 * @param nombreVendedor  Nombre completo del vendedor asignado (puede ser null).
 * @param nombre          Nombre del prospecto.
 * @param email           Email del prospecto.
 * @param telefono        Teléfono del prospecto.
 * @param estado          Estado actual en el pipeline.
 * @param score           Puntuación de calificación (0–100).
 * @param createdAt       Fecha y hora de creación.
 */
public record LeadResponse(
        UUID id,
        UUID tenantId,
        UUID vendedorId,
        String nombreVendedor,
        String nombre,
        String email,
        String telefono,
        EstadoLead estado,
        Integer score,
        LocalDateTime createdAt
) {}
