import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { InteraccionCRM, InteraccionCreateRequest } from '../models/crm.models';

const API_BASE = 'http://localhost:8080/api/v1/crm/interacciones';

/**
 * Servicio para consumir los endpoints de la Bitácora Comercial (InteraccionCRM).
 *
 * Los headers `Authorization: Bearer <token>` se inyectan automáticamente
 * por el `jwtInterceptor` configurado en `app.config.ts`.
 */
@Injectable({ providedIn: 'root' })
export class InteraccionCrmService {
  private readonly http = inject(HttpClient);

  /**
   * Recupera el historial de interacciones de un Lead, del más reciente al
   * más antiguo.
   *
   * GET /api/v1/crm/interacciones/lead/{leadId}
   */
  getInteraccionesPorLead(leadId: string): Observable<InteraccionCRM[]> {
    return this.http.get<InteraccionCRM[]>(`${API_BASE}/lead/${leadId}`);
  }

  /**
   * Registra una nueva actividad comercial sobre un Lead.
   * Si el lead estaba en estado NUEVO, el backend lo promoverá a CONTACTADO.
   *
   * POST /api/v1/crm/interacciones/lead/{leadId}
   */
  crearInteraccionLead(
    leadId: string,
    request: InteraccionCreateRequest
  ): Observable<InteraccionCRM> {
    return this.http.post<InteraccionCRM>(`${API_BASE}/lead/${leadId}`, request);
  }
}
