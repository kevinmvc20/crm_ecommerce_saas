package com.saas.crm.dto.saas;

import java.math.BigDecimal;

/**
 * DTO de respuesta que representa un plan SaaS disponible en la plataforma.
 *
 * @param id              identificador del plan
 * @param nombre          nombre comercial del plan
 * @param precioMensual   precio mensual en USD
 * @param limiteUsuarios  número máximo de usuarios permitidos
 * @param limiteProductos número máximo de productos permitidos
 * @param activo          indica si el plan está disponible para contratación
 */
public record PlanSaaSDTO(
                Integer id,
                String nombre,
                BigDecimal precioMensual,
                Integer limiteUsuarios,
                Integer limiteProductos,
                Boolean activo) {
}
