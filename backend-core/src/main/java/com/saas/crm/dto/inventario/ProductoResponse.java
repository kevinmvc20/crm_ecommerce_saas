package com.saas.crm.dto.inventario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Respuesta de la API con los datos completos de un producto y sus variantes.
 *
 * @param id          UUID del producto.
 * @param tenantId    UUID del tenant propietario.
 * @param categoriaId ID de la categoría (puede ser {@code null}).
 * @param nombre      Nombre del producto.
 * @param descripcion Descripción del producto.
 * @param activo      Estado de activación.
 * @param variantes   Lista de variantes de compra.
 * @param createdAt   Fecha de creación.
 * @param updatedAt   Fecha de última actualización.
 */
public record ProductoResponse(
        UUID id,
        UUID tenantId,
        Integer categoriaId,
        String nombre,
        String descripcion,
        Boolean activo,
        List<VarianteResponse> variantes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
