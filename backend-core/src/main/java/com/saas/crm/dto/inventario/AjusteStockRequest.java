package com.saas.crm.dto.inventario;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Payload para ajustar el stock físico y el mínimo de una variante en una sucursal.
 *
 * @param nuevoStockFisico Nuevo valor absoluto del stock físico (debe ser &ge; 0).
 * @param stockMinimo      Nuevo umbral mínimo para alertas de reabastecimiento (&ge; 0).
 */
public record AjusteStockRequest(

        @NotNull(message = "El nuevo stock físico es obligatorio.")
        @Min(value = 0, message = "El stock físico no puede ser negativo.")
        Integer nuevoStockFisico,

        @NotNull(message = "El stock mínimo es obligatorio.")
        @Min(value = 0, message = "El stock mínimo no puede ser negativo.")
        Integer stockMinimo
) {}
