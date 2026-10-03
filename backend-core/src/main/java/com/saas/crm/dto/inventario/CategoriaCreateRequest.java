package com.saas.crm.dto.inventario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload para registrar una nueva categoría en el catálogo del tenant.
 *
 * @param nombre           Nombre de la categoría (requerido, máx. 100 caracteres).
 * @param descripcion      Descripción opcional de la categoría.
 * @param categoriaPadreId ID de la categoría padre; {@code null} para categorías raíz.
 */
public record CategoriaCreateRequest(

        @NotBlank(message = "El nombre de la categoría es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
        String nombre,

        String descripcion,

        Integer categoriaPadreId
) {}
