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

// ─── Oportunidades Comerciales (Pipeline Kanban) ──────────────────────────────

/**
 * Etapas del pipeline de ventas para una Oportunidad.
 * Debe coincidir con el enum EtapaOportunidad del backend.
 */
export type EtapaOportunidad =
  | 'CALIFICACION'
  | 'PROPUESTA'
  | 'NEGOCIACION'
  | 'GANADA'
  | 'PERDIDA';

/**
 * Representación pública de una Oportunidad Comercial devuelta por el backend.
 * Mapea el record OportunidadResponse de Java.
 */
export interface Oportunidad {
  id: string;
  clienteId: string;
  razonSocialCliente: string;
  vendedorId: string;
  nombreVendedor: string;
  /** Nombre del trato (alias de 'titulo' en la BD). */
  nombre: string;
  montoEstimado: number;
  probabilidad: number;  // 0–100
  etapa: EtapaOportunidad;
  /** Solo presente cuando etapa === 'PERDIDA'. */
  motivoPerdida: string | null;
  fechaCierreEsperada: string | null; // ISO-8601 date
  createdAt: string; // ISO-8601
}

/**
 * Payload para POST /api/v1/crm/oportunidades.
 */
export interface OportunidadCreateRequest {
  clienteId: string;
  nombre: string;
  montoEstimado: number;
  probabilidad: number;
  fechaCierreEsperada?: string; // ISO-8601 date YYYY-MM-DD
}

/**
 * Payload para PATCH /api/v1/crm/oportunidades/{id}/etapa.
 */
export interface CambioEtapaRequest {
  etapa: EtapaOportunidad;
}

/**
 * Payload para PATCH /api/v1/crm/oportunidades/{id}/perdida.
 */
export interface CerrarPerdidaRequest {
  motivo: string;
}

// ─── Clientes CRM ─────────────────────────────────────────────────────────────

/**
 * Representación pública de un Cliente devuelta por el backend.
 * Mapea el record ClienteResponse de Java.
 */
export interface ClienteCRM {
  id: string;
  razonSocialONombre: string;
  ciNit: string;
  email: string | null;
}
