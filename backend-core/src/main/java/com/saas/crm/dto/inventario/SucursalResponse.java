package com.saas.crm.dto.inventario;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta de la API con los datos de una sucursal.
 *
 * @param id        PK entero de la sucursal.
 * @param tenantId  UUID del tenant propietario.
 * @param nombre    Nombre de la sucursal.
 * @param direccion Dirección física.
 * @param telefono  Teléfono de contacto.
 * @param activo    Estado de activación.
 * @param createdAt Fecha de creación.
 * @param updatedAt Fecha de última actualización.
 */
public record SucursalResponse(
        Integer id,
        UUID tenantId,
        String nombre,
        String direccion,
        String telefono,
        Boolean activo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
