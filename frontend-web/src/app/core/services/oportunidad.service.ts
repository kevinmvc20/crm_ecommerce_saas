import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CambioEtapaRequest,
  CerrarPerdidaRequest,
  Oportunidad,
  OportunidadCreateRequest,
} from '../models/crm.models';

const API_BASE = 'http://localhost:8080/api/v1/crm/oportunidades';

/**
 * Servicio para consumir los endpoints del módulo CRM – Oportunidades (Pipeline Kanban).
 *
 * Los headers `Authorization: Bearer <token>` se inyectan automáticamente
 * por el `jwtInterceptor` configurado en `app.config.ts`.
 */
@Injectable({ providedIn: 'root' })
export class OportunidadService {
  private readonly http = inject(HttpClient);

  /**
   * Lista todas las oportunidades del tenant autenticado.
   *
   * GET /api/v1/crm/oportunidades
   */
  getOportunidades(): Observable<Oportunidad[]> {
    return this.http.get<Oportunidad[]>(API_BASE);
  }

  /**
   * Registra una nueva oportunidad en etapa CALIFICACION.
   *
   * POST /api/v1/crm/oportunidades
   */
  crear(data: OportunidadCreateRequest): Observable<Oportunidad> {
    return this.http.post<Oportunidad>(API_BASE, data);
  }

  /**
   * Cambia la etapa activa a CALIFICACION, PROPUESTA o NEGOCIACION.
   *
   * PATCH /api/v1/crm/oportunidades/{id}/etapa
   */
  cambiarEtapa(id: string, data: CambioEtapaRequest): Observable<Oportunidad> {
    return this.http.patch<Oportunidad>(`${API_BASE}/${id}/etapa`, data);
  }

  /**
   * Cierra la oportunidad como GANADA (probabilidad = 100 %).
   *
   * PATCH /api/v1/crm/oportunidades/{id}/ganada
   */
  cerrarGanada(id: string): Observable<Oportunidad> {
    return this.http.patch<Oportunidad>(`${API_BASE}/${id}/ganada`, {});
  }

  /**
   * Cierra la oportunidad como PERDIDA con motivo obligatorio.
   *
   * PATCH /api/v1/crm/oportunidades/{id}/perdida
   */
  cerrarPerdida(id: string, data: CerrarPerdidaRequest): Observable<Oportunidad> {
    return this.http.patch<Oportunidad>(`${API_BASE}/${id}/perdida`, data);
  }
}
