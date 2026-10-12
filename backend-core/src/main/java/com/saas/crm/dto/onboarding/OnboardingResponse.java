package com.saas.crm.dto.onboarding;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO inmutable de respuesta devuelto al completar el registro self-service.
 */
public record OnboardingResponse(
        UUID tenantId,
        String nombreComercial,
        String subdominio,
        String adminEmail,
        String planNombre,
        String estadoSuscripcion,
        String transaccionId,
        LocalDateTime fechaVencimientoPlan,
        String mensaje
) {}