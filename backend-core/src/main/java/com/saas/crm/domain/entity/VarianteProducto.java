package com.saas.crm.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code variante_producto}.
 *
 * <p>Una variante representa una presentación de compra concreta de un
 * {@link Producto} (p.e. talla, color, capacidad). Cada variante tiene un
 * SKU único dentro del tenant.</p>
 */
@Entity
@Table(
    name = "variante_producto",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_variante_tenant_sku",
        columnNames = {"tenant_id", "sku"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class VarianteProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Tenant propietario de la variante (aislamiento multi-tenant). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Producto padre al que pertenece esta variante. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "sku", nullable = false, length = 60)
    private String sku;

    @Column(name = "nombre_variante", nullable = false, length = 100)
    private String nombreVariante;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio = BigDecimal.ZERO;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

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
}
