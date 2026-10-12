package com.saas.crm.dto.saas;

import java.time.LocalDateTime;

/**
 * DTO de solo lectura con las métricas de consumo del tenant autenticado.
 *
 * <p>Expuesto vía {@code GET /api/v1/admin/tenant/consumo} para que el frontend
 * pueda pintar barras de uso y activar alertas preventivas.</p>
 *
 * <h3>Semántica de límites:</h3>
 * <ul>
 *   <li>{@code -1} en cualquier campo {@code *Limite} indica cuota <b>ilimitada</b>.</li>
 *   <li>Los flags {@code limite*Alcanzado} solo son {@code true} cuando el límite
 *       es finito ({@code > 0}) y se ha alcanzado o superado.</li>
 * </ul>
 *
 * @param planNombre               nombre del plan SaaS activo (Básico, Pro, Business)
 * @param estadoSuscripcion        estado de la suscripción (ACTIVO, SUSPENDIDO, etc.)
 * @param diasRestantesMembresia   días entre ahora y {@code fechaVencimientoPlan}; 0 si ya venció o es nula
 * @param fechaVencimientoPlan     fecha/hora exacta de vencimiento del plan
 * @param usuariosActuales         usuarios activos actuales del tenant
 * @param usuariosLimite           límite de usuarios del plan (-1 si ilimitado)
 * @param productosActuales        productos activos actuales del tenant
 * @param productosLimite          límite de productos del plan (-1 si ilimitado)
 * @param sucursalesActuales       sucursales activas actuales del tenant
 * @param sucursalesLimite         límite de sucursales del plan (1 para Básico/Pro, -1 para Business)
 * @param limiteUsuariosAlcanzado  {@code true} si el tenant ha llegado al tope de usuarios
 * @param limiteProductosAlcanzado {@code true} si el tenant ha llegado al tope de productos
 */
public record TenantConsumoResponse(
        String planNombre,
        String estadoSuscripcion,
        long diasRestantesMembresia,
        LocalDateTime fechaVencimientoPlan,
        long usuariosActuales,
        int usuariosLimite,
        long productosActuales,
        int productosLimite,
        long sucursalesActuales,
        int sucursalesLimite,
        boolean limiteUsuariosAlcanzado,
        boolean limiteProductosAlcanzado
) {}
