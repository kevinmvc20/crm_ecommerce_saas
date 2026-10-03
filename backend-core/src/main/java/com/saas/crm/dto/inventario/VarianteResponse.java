package com.saas.crm.dto.inventario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta de la API con los datos de una variante de producto.
 *
 * @param id             UUID de la variante.
 * @param productoId     UUID del producto padre.
 * @param sku            Código de referencia.
 * @param nombreVariante Nombre descriptivo de la variante.
 * @param precio         Precio de venta.
 * @param activo         Estado de activación.
 * @param createdAt      Fecha de creación.
 * @param updatedAt      Fecha de última actualización.
 */
public record VarianteResponse(
        UUID id,
        UUID productoId,
        String sku,
        String nombreVariante,
        BigDecimal precio,
        Boolean activo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
