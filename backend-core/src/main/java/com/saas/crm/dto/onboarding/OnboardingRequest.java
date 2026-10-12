package com.saas.crm.dto.onboarding;

import jakarta.validation.constraints.*;

/**
 * DTO inmutable de entrada para el registro self-service de una nueva empresa inquilina.
 */
public record OnboardingRequest(

        @NotBlank(message = "El nombre comercial es obligatorio")
        String nombreComercial,

        @NotBlank(message = "El subdominio es obligatorio")
        @Pattern(
                regexp = "^[a-z0-9]([a-z0-9\\-]{0,78}[a-z0-9])?$",
                message = "El subdominio debe ser alfanumerico en minusculas y sin espacios"
        )
        String subdominio,

        @NotNull(message = "Debe seleccionar un plan SaaS")
        Integer planSaaSId,

        @NotBlank(message = "El nombre completo del administrador es obligatorio")
        String adminNombreCompleto,

        @NotBlank(message = "El email del administrador es obligatorio")
        @Email(message = "Debe ingresar un email valido")
        String adminEmail,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres")
        String adminPassword,

        @NotBlank(message = "El metodo de pago es obligatorio")
        String metodoPago,

        String numeroTarjeta,
        String titular,
        String expiracion,
        String cvv
) {}