package com.saas.crm.dto.inventario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SucursalUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.")
        String nombre,
        @Size(max = 250, message = "La dirección no puede superar los 250 caracteres.")
        String direccion,
        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres.")
        String telefono
) {}
