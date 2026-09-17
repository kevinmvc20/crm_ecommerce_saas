package com.saas.crm.dto.crm;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload para la conversión de un Lead a Cliente.
 *
 * @param ciNit               CI o NIT fiscal del nuevo cliente (requerido).
 * @param razonSocialONombre  Razón social o nombre completo del cliente (requerido).
 */
public record LeadConvertirRequest(

        @NotBlank(message = "El CI/NIT es obligatorio para la conversión.")
        String ciNit,

        @NotBlank(message = "La razón social o nombre es obligatoria.")
        String razonSocialONombre
) {}
