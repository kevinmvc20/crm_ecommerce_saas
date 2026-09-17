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

// ─── Bitácora Comercial (InteraccionCRM) ──────────────────────────────────────

/**
 * Tipos de actividad registrables en la bitácora comercial.
 * Debe coincidir con el enum TipoInteraccionCRM del backend.
 */
export type TipoInteraccion = 'LLAMADA' | 'CORREO' | 'REUNION' | 'NOTA';

/**
 * Representación pública de una interacción CRM devuelta por el backend.
 * Mapea el record InteraccionResponse de Java.
 */
export interface InteraccionCRM {
  id: string;
  tenantId: string;
  leadId: string | null;
  clienteId: string | null;
  ejecutivoId: string;
  nombreEjecutivo: string;
  tipo: TipoInteraccion;
  descripcion: string;
  fechaHora: string; // ISO-8601
}

/**
 * Payload para POST /api/v1/crm/interacciones/lead/{leadId}.
 */
export interface InteraccionCreateRequest {
  tipo: TipoInteraccion;
  descripcion: string;
}
