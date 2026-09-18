package com.saas.crm.dto.crm;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload de creación de una Oportunidad Comercial.
 *
 * @param clienteId           UUID del cliente asociado (requerido).
 * @param nombre              Nombre descriptivo del trato (requerido).
 * @param montoEstimado       Valor estimado de la oportunidad (≥ 0).
 * @param probabilidad        Probabilidad de cierre 0–100 (requerido).
 * @param fechaCierreEsperada Fecha objetivo de cierre (opcional).
 */
public record OportunidadCreateRequest(

        @NotNull(message = "El clienteId es obligatorio.")
        UUID clienteId,

        @NotBlank(message = "El nombre de la oportunidad es obligatorio.")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres.")
        String nombre,

        @NotNull(message = "El monto estimado es obligatorio.")
        @DecimalMin(value = "0.00", message = "El monto estimado no puede ser negativo.")
        BigDecimal montoEstimado,

        @NotNull(message = "La probabilidad es obligatoria.")
        @Min(value = 0, message = "La probabilidad mínima es 0.")
        @Max(value = 100, message = "La probabilidad máxima es 100.")
        Integer probabilidad,

        LocalDate fechaCierreEsperada
) {}
