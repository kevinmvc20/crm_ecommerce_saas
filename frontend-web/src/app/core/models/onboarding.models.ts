/**
 * Payload enviado al endpoint POST /api/v1/public/onboarding/register
 * para registrar una nueva empresa de forma self-service.
 */
export interface OnboardingRequest {
  nombreComercial: string;
  subdominio: string;
  planSaaSId: number;
  adminNombreCompleto: string;
  adminEmail: string;
  adminPassword: string;
  metodoPago: 'TARJETA' | 'QR';
  /** Presente solo si metodoPago === 'TARJETA' */
  numeroTarjeta?: string;
  titular?: string;
  expiracion?: string;
  cvv?: string;
}

/**
 * Respuesta exitosa del endpoint de registro público de empresa.
 */
export interface OnboardingResponse {
  tenantId: string;
  nombreComercial: string;
  subdominio: string;
  adminEmail: string;
  planNombre: string;
  estadoSuscripcion: string;
  transaccionId: string;
  fechaVencimientoPlan: string;
  mensaje: string;
}
