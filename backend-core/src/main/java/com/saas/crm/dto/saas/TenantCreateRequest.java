package com.saas.crm.dto.saas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Payload de entrada para crear un nuevo Tenant (empresa inquilina).
 *
 * @param nombreComercial nombre visible de la empresa
 * @param subdominio      identificador único de acceso (solo minúsculas, dígitos y guiones)
 * @param planSaaSId      ID del plan SaaS que se asignará al tenant
 */
public record TenantCreateRequest(

        @NotBlank(message = "El nombre comercial es obligatorio.")
        String nombreComercial,

        @NotBlank(message = "El subdominio es obligatorio.")
        @Pattern(
                regexp = "^[a-z0-9]([a-z0-9\\-]{0,78}[a-z0-9])?$",
                message = "El subdominio solo puede contener minúsculas, dígitos y guiones, " +
                          "sin comenzar ni terminar con guión, y máximo 80 caracteres."
        )
        String subdominio,

        @NotNull(message = "Debe seleccionar un plan SaaS.")
        Integer planSaaSId

) {}
