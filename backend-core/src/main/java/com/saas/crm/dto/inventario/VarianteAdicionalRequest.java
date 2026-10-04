package com.saas.crm.dto.inventario;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload para agregar una variante adicional a un producto ya existente.
 *
 * <p>A diferencia de {@link VarianteCreateRequest} (que forma parte del flujo
 * de creación completa de un producto), este DTO permite añadir variantes
 * de forma incremental sin duplicar el producto base.</p>
 *
 * @param sku            Código de referencia único en el tenant (obligatorio).
 * @param nombreVariante Nombre descriptivo de la variante (obligatorio, máx. 100 chars).
 * @param precio         Precio de venta de la variante (&ge; 0).
 * @param stockInicial   Unidades físicas iniciales en la sucursal indicada (opcional).
 * @param sucursalId     ID de la sucursal donde inicializar el stock
 *                       (obligatorio si {@code stockInicial} &gt; 0).
 */
public record VarianteAdicionalRequest(

        @NotBlank(message = "El SKU de la variante es obligatorio.")
        @Size(max = 60, message = "El SKU no puede superar los 60 caracteres.")
        String sku,

        @NotBlank(message = "El nombre de la variante es obligatorio.")
        @Size(max = 100, message = "El nombre de la variante no puede superar los 100 caracteres.")
        String nombreVariante,

        @NotNull(message = "El precio es obligatorio.")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo.")
        BigDecimal precio,

        @Min(value = 0, message = "El stock inicial no puede ser negativo.")
        Integer stockInicial,

        Integer sucursalId
) {}
