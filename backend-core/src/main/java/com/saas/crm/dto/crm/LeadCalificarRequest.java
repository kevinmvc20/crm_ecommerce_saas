package com.saas.crm.dto.crm;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Payload para la calificación de un Lead.
 *
 * @param score  Puntuación de calificación entre 0 y 100.
 * @param notas  Observaciones del proceso de calificación (opcional).
 */
public record LeadCalificarRequest(

        @Min(value = 0, message = "El score mínimo es 0.")
        @Max(value = 100, message = "El score máximo es 100.")
        Integer score,

        String notas
) {}
