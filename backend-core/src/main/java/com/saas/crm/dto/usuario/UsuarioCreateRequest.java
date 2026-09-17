package com.saas.crm.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada para aprovisionar un nuevo usuario operacional en un Tenant.
 *
 * @param nombreCompleto nombre completo del usuario
 * @param email          dirección de correo electrónico (debe ser única globalmente)
 * @param password       contraseña en texto plano (mínimo 6 caracteres); se almacenará hasheada con BCrypt
 * @param rolNombre      nombre del rol a asignar; únicamente "ROLE_ADMIN_EMPRESA" o "ROLE_VENDEDOR"
 */
public record UsuarioCreateRequest(

        @NotBlank(message = "El nombre completo es obligatorio")
        String nombreCompleto,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password,

        @NotBlank(message = "El rol es obligatorio")
        @Pattern(
                regexp = "ROLE_ADMIN_EMPRESA|ROLE_VENDEDOR",
                message = "El rol debe ser 'ROLE_ADMIN_EMPRESA' o 'ROLE_VENDEDOR'"
        )
        String rolNombre
) {}
