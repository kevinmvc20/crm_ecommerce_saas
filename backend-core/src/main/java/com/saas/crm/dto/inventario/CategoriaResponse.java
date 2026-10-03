package com.saas.crm.dto.inventario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Respuesta de la API con los datos de una categoría.
 * Incluye las subcategorías hijas para representar el árbol completo.
 *
 * @param id               PK de la categoría.
 * @param tenantId         UUID del tenant propietario.
 * @param categoriaPadreId ID del padre; {@code null} si es raíz.
 * @param nombre           Nombre de la categoría.
 * @param descripcion      Descripción.
 * @param activo           Estado de activación.
 * @param subcategorias    Lista de subcategorías hijas (puede estar vacía).
 * @param createdAt        Fecha de creación.
 */
public record CategoriaResponse(
        Integer id,
        UUID tenantId,
        Integer categoriaPadreId,
        String nombre,
        String descripcion,
        Boolean activo,
        List<CategoriaResponse> subcategorias,
        LocalDateTime createdAt
) {}
