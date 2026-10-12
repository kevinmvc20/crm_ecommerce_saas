import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TenantConsumo } from '../models/consumo.models';

const API_BASE = 'http://localhost:8080/api/v1/admin/tenant';

/**
 * Servicio para consultar el consumo de recursos del tenant autenticado.
 *
 * Requiere JWT con rol ROLE_ADMIN_EMPRESA. El Bearer token se inyecta
 * automáticamente por el jwtInterceptor configurado en app.config.ts.
 */
@Injectable({ providedIn: 'root' })
export class ConsumoService {
  private readonly http = inject(HttpClient);

  /**
   * Obtiene el estado de consumo y cuotas del tenant activo.
   * Endpoint: GET /api/v1/admin/tenant/consumo
   */
  getConsumoActual(): Observable<TenantConsumo> {
    return this.http.get<TenantConsumo>(`${API_BASE}/consumo`);
  }
}
