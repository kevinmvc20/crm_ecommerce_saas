package com.saas.crm.domain.entity;

import com.saas.crm.domain.enums.EtapaOportunidad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ─── Lifecycle callbacks ──────────────────────────────────────────────────

    /** Inicializa {@code createdAt} y {@code updatedAt} antes de la primera persistencia. */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.etapa == null) this.etapa = EtapaOportunidad.CALIFICACION;
    }

    /** Actualiza {@code updatedAt} en cada modificación. */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Métodos de negocio ───────────────────────────────────────────────────

    /**
     * Verifica si la oportunidad está en un estado terminal (GANADA o PERDIDA).
     * Los estados terminales no pueden ser modificados.
     */
    public boolean isTerminal() {
        return this.etapa == EtapaOportunidad.GANADA
                || this.etapa == EtapaOportunidad.PERDIDA;
    }

    /**
     * Avanza la oportunidad a la etapa indicada.
     * No puede usarse para cerrar la oportunidad (usar {@link #cerrarGanada()}
     * o {@link #cerrarPerdida(String)} en su lugar).
     *
     * @param nuevaEtapa nueva etapa a establecer
     * @throws ResponseStatusException HTTP 400 si ya está en estado terminal
     * @throws ResponseStatusException HTTP 400 si se intenta usar para cierre
     */
    public void avanzarEtapa(EtapaOportunidad nuevaEtapa) {
        if (isTerminal()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La oportunidad ya está cerrada (etapa: " + this.etapa + ") y no puede modificarse.");
        }
        if (nuevaEtapa == EtapaOportunidad.GANADA) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Para cerrar como ganada, use el endpoint PATCH /{id}/ganada.");
        }
        if (nuevaEtapa == EtapaOportunidad.PERDIDA) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Para cerrar como perdida (con motivo), use el endpoint PATCH /{id}/perdida.");
        }
        this.etapa = nuevaEtapa;
    }

    /**
     * Cierra la oportunidad como GANADA.
     * Establece probabilidad al 100 %.
     *
     * @throws ResponseStatusException HTTP 400 si ya está en estado terminal
     */
    public void cerrarGanada() {
        if (isTerminal()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La oportunidad ya está cerrada (etapa: " + this.etapa + ") y no puede modificarse.");
        }
        this.etapa = EtapaOportunidad.GANADA;
        this.probabilidad = 100;
    }

    /**
     * Cierra la oportunidad como PERDIDA con el motivo indicado.
     * Establece probabilidad al 0 %.
     *
     * @param motivo descripción del motivo de pérdida (obligatorio)
     * @throws ResponseStatusException HTTP 400 si motivo está vacío
     * @throws ResponseStatusException HTTP 400 si ya está en estado terminal
     */
    public void cerrarPerdida(String motivo) {
        if (isTerminal()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La oportunidad ya está cerrada (etapa: " + this.etapa + ") y no puede modificarse.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El motivo de pérdida es obligatorio para cerrar la oportunidad como PERDIDA.");
        }
        this.etapa = EtapaOportunidad.PERDIDA;
        this.probabilidad = 0;
        this.motivoCierre = motivo.trim();
    }
}
