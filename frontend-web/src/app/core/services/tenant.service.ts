import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PlanSaaS,
  Tenant,
  TenantCreateRequest,
  UsuarioTenant,
  UsuarioTenantCreateRequest,
} from '../models/tenant.models';

const API_BASE = 'http://localhost:8080/api/v1';

/**
 * Servicio para consumo de los endpoints de Planes y Tenants.
 *
 * Los headers `Authorization: Bearer <token>` y `X-Tenant-Id` se inyectan
 * automáticamente por el `jwtInterceptor` configurado en `app.config.ts`.
 * No se necesita ningún header adicional en este servicio.
 */
@Injectable({ providedIn: 'root' })
export class TenantService {
  private readonly http = inject(HttpClient);

  /**
   * Obtiene la lista de planes SaaS activos.
   * Endpoint: GET /api/v1/planes
   */
  getPlanes(): Observable<PlanSaaS[]> {
    return this.http.get<PlanSaaS[]>(`${API_BASE}/planes`);
  }

  /**
   * Obtiene todos los tenants registrados en la plataforma.
   * Endpoint: GET /api/v1/tenants — requiere ROLE_SUPER_ADMIN.
   */
  getTenants(): Observable<Tenant[]> {
    return this.http.get<Tenant[]>(`${API_BASE}/tenants`);
  }

  /**
   * Registra un nuevo tenant (empresa cliente) en la plataforma.
   * Endpoint: POST /api/v1/tenants — requiere ROLE_SUPER_ADMIN.
   */
  createTenant(data: TenantCreateRequest): Observable<Tenant> {
    return this.http.post<Tenant>(`${API_BASE}/tenants`, data);
  }

  /**
   * Lista los usuarios internos de un tenant.
   * Endpoint: GET /api/v1/tenants/{tenantId}/usuarios
   */
  getUsuariosByTenant(tenantId: string): Observable<UsuarioTenant[]> {
    return this.http.get<UsuarioTenant[]>(`${API_BASE}/tenants/${tenantId}/usuarios`);
  }

  /**
   * Da de alta un usuario interno asignado a la empresa.
   * Endpoint: POST /api/v1/tenants/{tenantId}/usuarios
   */
  crearUsuarioTenant(
    tenantId: string,
    data: UsuarioTenantCreateRequest,
  ): Observable<UsuarioTenant> {
    return this.http.post<UsuarioTenant>(`${API_BASE}/tenants/${tenantId}/usuarios`, data);
  }
}
