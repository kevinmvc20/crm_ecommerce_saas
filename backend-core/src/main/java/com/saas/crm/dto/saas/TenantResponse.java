package com.saas.crm.dto.saas;

import java.util.UUID;

/**
 * DTO de respuesta que representa un Tenant registrado en la plataforma.
 *
 * @param id                  identificador UUID del tenant
 * @param nombreComercial     nombre visible de la empresa
 * @param subdominio          subdominio único de acceso
 * @param estadoSuscripcion   estado actual de la suscripción (p.ej. "ACTIVO")
 * @param planNombre          nombre del plan SaaS asignado
 * @param activo              indica si el tenant está habilitado
 */
public record TenantResponse(
        UUID id,
        String nombreComercial,
        String subdominio,
        String estadoSuscripcion,
        String planNombre,
        Boolean activo
) {}
