/**
 * Plan SaaS disponible para asignar a un tenant.
 * El id es numérico (PK autoincremental en el backend).
 */
export interface PlanSaaS {
  id: number;
  nombre: string;
  precioMensual: number;
  limiteUsuarios: number;
  limiteProductos: number;
}

/**
 * Tenant (empresa cliente) registrado en la plataforma.
 * El id es UUID generado por Spring Boot — se representa como string.
 */
export interface Tenant {
  id: string;
  nombreComercial: string;
  subdominio: string;
  estadoSuscripcion: string;
  planNombre: string;
  activo: boolean;
}

/**
 * Payload enviado al endpoint POST /api/v1/tenants
 * para registrar una nueva empresa en la plataforma.
 */
export interface TenantCreateRequest {
  nombreComercial: string;
  subdominio: string;
  planSaaSId: number;
}

/**
 * Usuario interno de un tenant, devuelto por
 * GET /api/v1/tenants/{tenantId}/usuarios
 */
export interface UsuarioTenant {
  id: string;
  tenantId: string;
  nombreCompleto: string;
  email: string;
  rolNombre: string;
  activo: boolean;
  createdAt: string;
}

/**
 * Payload enviado al endpoint POST /api/v1/tenants/{tenantId}/usuarios
 * para dar de alta un usuario interno en una empresa.
 */
export interface UsuarioTenantCreateRequest {
  nombreCompleto: string;
  email: string;
  password: string;
  rolNombre: string;
}
