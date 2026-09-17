package com.saas.crm.domain.entity;

import com.saas.crm.domain.enums.TipoInteraccionCRM;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code interaccion_crm}.
 *
 * <p>Registra cada punto de contacto o actividad (llamada, reunión, correo, nota)
 * realizada por un ejecutivo hacia un {@link Lead} o un {@link Cliente}.</p>
 */
@Entity
@Table(name = "interaccion_crm")
@Getter
@Setter
@NoArgsConstructor
public class InteraccionCRM {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Tenant al que pertenece la interacción (cargado de forma diferida). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /**
     * Cliente asociado a la interacción; puede ser null si la interacción
     * es únicamente con un lead todavía no convertido.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;

    /**
     * Lead asociado a la interacción; puede ser null si la interacción
     * es con un cliente ya convertido.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = true)
    private Lead lead;

    /** Ejecutivo (vendedor) que registró la interacción (cargado de forma diferida). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ejecutivo_id", nullable = false)
    private Usuario ejecutivo;

    /**
     * Tipo de interacción.
     * Mapeado como String para legibilidad en la base de datos.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoInteraccionCRM tipo;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    /** Inicializa {@code fechaHora} automáticamente antes de la primera persistencia. */
    @PrePersist
    protected void onCreate() {
        if (this.fechaHora == null) {
            this.fechaHora = LocalDateTime.now();
        }
    }
}
