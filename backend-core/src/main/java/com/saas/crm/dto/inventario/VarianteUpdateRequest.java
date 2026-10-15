package com.saas.crm.dto.inventario;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Payload para actualizar los datos comerciales de una variante de producto.
 *
 * <p>Permite modificar el nombre comercial de la variante, el precio de venta y el
 * estado activo. El código SKU es estrictamente inmutable para garantizar la
 * trazabilidad del inventario y la integridad referencial.</p>
 *
 * @param nombreVariante Nombre comercial de la variante (obligatorio, máx. 100 chars).
 * @param precio         Precio de venta unitario (obligatorio, no negativo).
 * @param activo         Estado de disponibilidad/activación (obligatorio).
 */
public record VarianteUpdateRequest(
    @NotBlank(message = "El nombre de la variante es obligatorio.")
    @Size(max = 100, message = "El nombre de la variante no puede superar los 100 caracteres.")
    String nombreVariante,

    @NotNull(message = "El precio es obligatorio.")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo.")
    BigDecimal precio,

    @NotNull(message = "El estado activo es obligatorio.")
    Boolean activo
) {}
