package com.saas.crm.dto.crm;

import com.saas.crm.domain.enums.TipoInteraccionCRM;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload de entrada para registrar una nueva interacción comercial
 * sobre un Lead o un Cliente.
 *
 * @param tipo        Tipo de actividad (LLAMADA, CORREO, REUNION, NOTA). Requerido.
 * @param descripcion Detalle narrativo de la actividad realizada. Requerido.
 */
public record InteraccionCreateRequest(

        @NotNull(message = "El tipo de interacción es requerido.")
        TipoInteraccionCRM tipo,

        @NotBlank(message = "La descripción de la interacción no puede estar vacía.")
        String descripcion
) {}
