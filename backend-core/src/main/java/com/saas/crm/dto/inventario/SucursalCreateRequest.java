package com.saas.crm.dto.inventario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload para registrar una nueva sucursal del tenant autenticado.
 *
 * @param nombre    Nombre de la sucursal (requerido, máx. 120 caracteres).
 * @param direccion Dirección física de la sucursal (opcional).
 * @param telefono  Teléfono de contacto (opcional, máx. 30 caracteres).
 */
public record SucursalCreateRequest(

        @NotBlank(message = "El nombre de la sucursal es obligatorio.")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.")
        String nombre,

        @Size(max = 250, message = "La dirección no puede superar los 250 caracteres.")
        String direccion,

        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres.")
        String telefono
) {}
