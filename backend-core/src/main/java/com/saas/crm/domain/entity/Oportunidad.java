package com.saas.crm.domain.entity;

import com.saas.crm.domain.enums.EtapaOportunidad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code oportunidad} (pipeline de ventas).
 *
 * <p>Representa una oportunidad de negocio vinculada a un {@link Cliente} y
 * gestionada por un vendedor. Avanza a través de las etapas definidas en
 * {@link EtapaOportunidad}.</p>
 */
@Entity
@Table(name = "oportunidad")
@Getter
@Setter
@NoArgsConstructor
public class Oportunidad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Tenant propietario de la oportunidad (cargado de forma diferida). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Cliente al que pertenece la oportunidad (cargado de forma diferida). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    /** Vendedor responsable de la oportunidad (cargado de forma diferida). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "monto_estimado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoEstimado = BigDecimal.ZERO;

    /**
     * Etapa actual en el pipeline de ventas.
     * Mapeado como String para legibilidad en la base de datos.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "etapa", nullable = false, length = 50)
    private EtapaOportunidad etapa = EtapaOportunidad.CALIFICACION;

    /** Probabilidad de cierre estimada (0–100). */
    @Column(name = "probabilidad", nullable = false)
    private Integer probabilidad = 10;

    @Column(name = "motivo_cierre", columnDefinition = "TEXT")
    private String motivoCierre;

    @Column(name = "fecha_cierre_estimada")
    private LocalDate fechaCierreEstimada;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Inicializa {@code createdAt} automáticamente antes de la primera persistencia. */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.etapa == null) {
            this.etapa = EtapaOportunidad.CALIFICACION;
        }
    }
}
