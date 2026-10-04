package com.saas.crm.dto.inventario;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Payload para crear una variante de producto dentro de un {@link ProductoCreateRequest}.
 *
 * @param sku               Código de referencia único dentro del tenant (requerido, máx. 60 caracteres).
 * @param nombreVariante    Nombre descriptivo de la variante (requerido, máx. 100 caracteres).
 * @param precio            Precio de venta; debe ser &ge; 0 (requerido).
 * @param stockInicial      Unidades físicas iniciales de stock (opcional; si se provee, se crea el registro de stock).
 * @param sucursalIdInicial ID de la sucursal donde inicializar el stock (requerido si {@code stockInicial} &gt; 0).
 */
public record VarianteCreateRequest(

        @NotBlank(message = "El SKU es obligatorio.")
        @Size(max = 60, message = "El SKU no puede superar los 60 caracteres.")
        String sku,

        @NotBlank(message = "El nombre de la variante es obligatorio.")
        @Size(max = 100, message = "El nombre de la variante no puede superar los 100 caracteres.")
        String nombreVariante,

        @NotNull(message = "El precio es obligatorio.")
        @DecimalMin(value = "0.00", message = "El precio debe ser mayor o igual a 0.")
        BigDecimal precio,

        Integer stockInicial,

        Integer sucursalIdInicial,
        
        Integer sucursalId
) {
    public Integer getSucursalEfectiva() {
        return sucursalId != null ? sucursalId : sucursalIdInicial;
    }
}
