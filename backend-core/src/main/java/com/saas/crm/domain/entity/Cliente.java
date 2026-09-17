package com.saas.crm.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code cliente}.
 *
 * <p>Un Cliente puede originarse desde un {@link Lead} convertido o ser
 * registrado directamente. Puede vincularse a un {@link Usuario} del sistema
 * (portal de compradores) a través de {@code usuarioIdAuth}.</p>
 */
@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Tenant propietario del cliente (cargado de forma diferida). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /**
     * Usuario de autenticación vinculado al cliente (portal de compras).
     * Puede ser null si el cliente no tiene acceso al portal.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id_auth", nullable = true)
    private Usuario usuarioAuth;

    /**
     * Lead de origen si el cliente fue creado mediante conversión.
     * Nulo para clientes registrados directamente.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id_origen", nullable = true)
    private Lead leadOrigen;

    @Column(name = "razon_social_o_nombre", nullable = false, length = 200)
    private String razonSocialONombre;

    @Column(name = "ci_nit", nullable = false, length = 50)
    private String ciNit;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "telefono", length = 50)
    private String telefono;

    /** Customer Lifetime Value acumulado; se actualiza en cada compra. */
    @Column(name = "clv_acumulado", nullable = false, precision = 12, scale = 2)
    private BigDecimal clvAcumulado = BigDecimal.ZERO;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Inicializa {@code createdAt} automáticamente antes de la primera persistencia. */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
