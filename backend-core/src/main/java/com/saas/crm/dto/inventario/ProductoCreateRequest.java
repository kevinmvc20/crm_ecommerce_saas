package com.saas.crm.dto.inventario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload para registrar un nuevo producto con sus variantes de compra.
 *
 * @param nombre       Nombre del producto (requerido, máx. 150 caracteres).
 * @param descripcion  Descripción opcional del producto.
 * @param categoriaId  ID de la categoría a la que pertenece (opcional).
 * @param variantes    Lista de variantes; debe contener al menos una variante.
 */
public record ProductoCreateRequest(

        @NotBlank(message = "El nombre del producto es obligatorio.")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres.")
        String nombre,

        String descripcion,

        Integer categoriaId,

        @NotEmpty(message = "El producto debe tener al menos una variante.")
        @Valid
        List<VarianteCreateRequest> variantes
) {}
