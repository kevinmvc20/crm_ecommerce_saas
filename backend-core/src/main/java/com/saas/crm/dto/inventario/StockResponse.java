package com.saas.crm.dto.inventario;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta de la API con el estado de stock de una variante en una sucursal.
 *
 * @param id                 PK del registro de stock.
 * @param sucursalId         ID de la sucursal.
 * @param varianteProductoId UUID de la variante de producto.
 * @param stockFisico        Unidades físicas en bodega.
 * @param stockReservado     Unidades reservadas por órdenes pendientes.
 * @param stockDisponible    Unidades disponibles para venta (físico - reservado).
 * @param stockMinimo        Umbral mínimo para alertas.
 * @param updatedAt          Fecha de última actualización.
 */
public record StockResponse(
        Long id,
        Integer sucursalId,
        UUID varianteProductoId,
        int stockFisico,
        int stockReservado,
        int stockDisponible,
        int stockMinimo,
        LocalDateTime updatedAt
) {}
