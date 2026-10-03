package com.saas.crm.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code stock_sucursal}.
 *
 * <p>Registra el inventario de una variante de producto en una sucursal
 * concreta. Distingue entre stock físico y reservado para evitar sobreventas.</p>
 *
 * <h3>Reglas de negocio embebidas:</h3>
 * <ul>
 *   <li>{@link #getStockDisponible()} — disponibilidad real = físico - reservado.</li>
 *   <li>{@link #reservar(int)} — reserva atómica con verificación de disponibilidad.</li>
 *   <li>{@link #liberar(int)} — libera reserva sin que baje de 0.</li>
 *   <li>{@link #consolidarVenta(int)} — descuenta reserva y físico al confirmar venta.</li>
 * </ul>
 */
@Entity
@Table(
    name = "stock_sucursal",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_stock_sucursal_variante",
        columnNames = {"sucursal_id", "variante_producto_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class StockSucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    /** Tenant propietario del registro de stock (aislamiento multi-tenant). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Sucursal a la que corresponde este stock. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    /** Variante de producto cuyo stock se registra. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variante_producto_id", nullable = false)
    private VarianteProducto varianteProducto;

    /** Unidades físicamente en depósito o bodega. */
    @Column(name = "stock_fisico", nullable = false)
    private int stockFisico = 0;

    /** Unidades reservadas por órdenes pendientes de confirmación. */
    @Column(name = "stock_reservado", nullable = false)
    private int stockReservado = 0;

    /** Umbral mínimo para alertas de reabastecimiento. */
    @Column(name = "stock_minimo", nullable = false)
    private int stockMinimo = 5;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Métodos de Negocio ───────────────────────────────────────────────────

    /**
     * Calcula el stock disponible para la venta.
     *
     * @return {@code stockFisico - stockReservado}
     */
    public int getStockDisponible() {
        return stockFisico - stockReservado;
    }

    /**
     * Intenta reservar {@code cant} unidades de forma atómica.
     *
     * @param cant cantidad a reservar (debe ser &gt; 0)
     * @return {@code true} si la reserva fue exitosa; {@code false} si no hay
     *         disponibilidad suficiente
     */
    public boolean reservar(int cant) {
        if (getStockDisponible() >= cant) {
            this.stockReservado += cant;
            return true;
        }
        return false;
    }

    /**
     * Libera {@code cant} unidades previamente reservadas.
     * El {@code stockReservado} nunca baja de 0.
     *
     * @param cant cantidad a liberar
     */
    public void liberar(int cant) {
        this.stockReservado = Math.max(0, this.stockReservado - cant);
    }

    /**
     * Consolida la venta: descuenta la reserva y el físico en {@code cant}.
     * Debe llamarse únicamente cuando la orden es confirmada/entregada.
     *
     * @param cant cantidad vendida y entregada
     */
    public void consolidarVenta(int cant) {
        this.stockReservado = Math.max(0, this.stockReservado - cant);
        this.stockFisico    = Math.max(0, this.stockFisico    - cant);
    }
}
