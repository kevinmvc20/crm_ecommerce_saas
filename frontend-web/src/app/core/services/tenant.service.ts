import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PlanSaaS, Tenant, TenantCreateRequest } from '../models/tenant.models';

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
}
