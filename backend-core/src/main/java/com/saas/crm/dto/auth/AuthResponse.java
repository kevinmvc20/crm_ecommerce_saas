package com.saas.crm.dto.auth;

import java.util.UUID;

/**
 * DTO de respuesta para el endpoint POST /api/v1/auth/login.
 * Encapsula el token JWT y los datos esenciales del usuario autenticado.
 */
public record AuthResponse(

        /** Token JWT firmado listo para usar en el header Authorization: Bearer &lt;token&gt;. */
        String token,

        String email,

        /** Nombre del rol (ej: ROLE_SUPER_ADMIN). */
        String rol,

        /** UUID del tenant, null para usuarios de plataforma (superadmin). */
        UUID tenantId,

        /** Nombre comercial del tenant, null para usuarios de plataforma. */
        String nombreTenant
) {}
