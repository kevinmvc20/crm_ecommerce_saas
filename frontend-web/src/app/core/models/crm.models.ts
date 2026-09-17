/**
 * Estados posibles de un Lead en el pipeline comercial.
 * Deben coincidir exactamente con el enum EstadoLead del backend.
 */
export type EstadoLead =
  | 'NUEVO'
  | 'CONTACTADO'
  | 'CALIFICADO'
  | 'DESCALIFICADO'
  | 'CONVERTIDO';

/**
 * Representación pública de un Lead devuelta por el backend.
 * Mapea el record LeadResponse de Java.
 */
export interface Lead {
  id: string;
  tenantId: string;
  vendedorId: string | null;
  nombreVendedor: string | null;
  nombre: string;
  email: string;
  telefono: string;
  estado: EstadoLead;
  /** Puntuación de calificación (0–100). null hasta que sea calificado. */
  score: number | null;
  createdAt: string; // ISO-8601
}

/**
 * Payload para POST /api/v1/crm/leads — registrar un nuevo prospecto.
 */
export interface LeadCreateRequest {
  nombre: string;
  email: string;
  telefono: string;
  vendedorId?: string;
}

/**
 * Payload para PUT /api/v1/crm/leads/{id}/calificar.
 */
export interface LeadCalificarRequest {
  score: number;  // 0–100
  notas: string;
}

/**
 * Payload para POST /api/v1/crm/leads/{id}/convertir.
 */
export interface LeadConvertirRequest {
  ciNit: string;
  razonSocialONombre: string;
}
