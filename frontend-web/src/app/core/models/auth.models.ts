/**
 * Payload enviado al endpoint POST /api/v1/auth/login
 */
export interface LoginRequest {
  email: string;
  password: string;
}

/**
 * Respuesta exitosa del backend tras autenticación
 */
export interface AuthResponse {
  token: string;
  email: string;
  rol: string;
  tenantId: string | null;
  nombreTenant: string | null;
}

/**
 * Estado de la sesión del usuario en memoria (Signals)
 */
export interface UserSession {
  email: string;
  rol: string;
  tenantId: string | null;
  nombreTenant: string | null;
}
