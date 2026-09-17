package com.saas.crm.domain.entity;

import com.saas.crm.domain.enums.EstadoLead;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code lead} (prospecto comercial).
 *
 * <p>Un Lead representa un contacto potencial que puede evolucionar a través
 * del pipeline CRM hasta convertirse en un {@link Cliente}.</p>
 */
@Entity
@Table(name = "lead")
@Getter
@Setter
@NoArgsConstructor
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Tenant al que pertenece el lead (cargado de forma diferida). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Vendedor asignado al lead; puede ser nulo si aún no está asignado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = true)
    private Usuario vendedorAsignado;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "telefono", length = 50)
    private String telefono;

    /**
     * Estado actual en el ciclo de vida del lead.
     * Mapeado como String (EnumType.STRING) para que Flyway y la BD mantengan
     * el valor legible sin depender del orden del enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 50)
    private EstadoLead estado = EstadoLead.NUEVO;

    @Column(name = "score", nullable = false)
    private Integer score = 0;

    @Column(name = "notas_calificacion", columnDefinition = "TEXT")
    private String notasCalificacion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Inicializa {@code createdAt} automáticamente antes de la primera persistencia. */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = EstadoLead.NUEVO;
        }
    }

    /** Actualiza {@code updatedAt} en cada modificación posterior. */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
