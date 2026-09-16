package com.saas.crm.domain.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA que mapea la tabla {@code plan_saas} definida en la migración V1.
 * Representa los planes comerciales disponibles en la plataforma SaaS.
 */
@Entity
@Table(name = "plan_saas")
@Getter
@Setter
@NoArgsConstructor
public class PlanSaaS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "precio_mensual", nullable = false)
    private BigDecimal precioMensual;

    @Column(name = "limite_usuarios", nullable = false)
    private Integer limiteUsuarios;

    @Column(name = "limite_productos", nullable = false)
    private Integer limiteProductos;

    @Column(name = "activo", nullable = false)
    private Boolean activo;
}
