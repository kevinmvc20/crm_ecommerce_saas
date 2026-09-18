package com.saas.crm.dto.crm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload para cerrar una Oportunidad como PERDIDA.
 * El motivo es obligatorio para garantizar trazabilidad comercial.
 *
 * @param motivo descripción del motivo de pérdida (requerido, mínimo 5 caracteres).
 */
public record CerrarPerdidaRequest(

        @NotBlank(message = "El motivo de pérdida es obligatorio.")
        @Size(min = 5, message = "El motivo debe tener al menos 5 caracteres.")
        String motivo
) {}
