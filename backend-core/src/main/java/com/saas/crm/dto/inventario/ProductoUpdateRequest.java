package com.saas.crm.dto.inventario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload para actualizar los datos descriptivos de un producto existente.
 *
 * <p>No permite reasignar variantes ni mover stock; esas operaciones tienen
 * sus propios endpoints. Solo actualiza nombre, descripción y categoría.</p>
 *
 * @param nombre      Nuevo nombre del producto (obligatorio, máx. 150 chars).
 * @param descripcion Nueva descripción del producto (opcional).
 * @param categoriaId ID de la categoría a la que se desea asociar el producto
 *                    ({@code null} para desasociar de cualquier categoría).
 */
public record ProductoUpdateRequest(

        @NotBlank(message = "El nombre del producto es obligatorio.")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres.")
        String nombre,

        String descripcion,

        Integer categoriaId
) {}
