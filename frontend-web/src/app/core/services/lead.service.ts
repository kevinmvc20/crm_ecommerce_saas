import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Lead,
  LeadCalificarRequest,
  LeadConvertirRequest,
  LeadCreateRequest,
} from '../models/crm.models';

const API_BASE = 'http://localhost:8080/api/v1/crm/leads';

/**
 * Servicio para consumir los endpoints del módulo CRM – Leads.
 *
 * Los headers `Authorization: Bearer <token>` se inyectan automáticamente
 * por el `jwtInterceptor` configurado en `app.config.ts`.
 */
@Injectable({ providedIn: 'root' })
export class LeadService {
  private readonly http = inject(HttpClient);

  /**
   * Lista los leads del tenant autenticado.
   * - ROLE_VENDEDOR: solo sus leads asignados.
   * - ROLE_ADMIN_EMPRESA: todos los leads del tenant.
   *
   * GET /api/v1/crm/leads
   */
  getLeads(): Observable<Lead[]> {
    return this.http.get<Lead[]>(API_BASE);
  }

  /**
   * Registra un nuevo Lead en estado NUEVO.
   *
   * POST /api/v1/crm/leads
   */
  crearLead(data: LeadCreateRequest): Observable<Lead> {
    return this.http.post<Lead>(API_BASE, data);
  }

  /**
   * Califica un Lead actualizando su score y notas; pasa el estado a CALIFICADO.
   *
   * PUT /api/v1/crm/leads/{id}/calificar
   */
  calificarLead(id: string, data: LeadCalificarRequest): Observable<Lead> {
    return this.http.put<Lead>(`${API_BASE}/${id}/calificar`, data);
  }

  /**
   * Convierte un Lead calificado en un Cliente.
   *
   * POST /api/v1/crm/leads/{id}/convertir
   */
  convertirACliente(id: string, data: LeadConvertirRequest): Observable<Lead> {
    return this.http.post<Lead>(`${API_BASE}/${id}/convertir`, data);
  }

  /**
   * Asigna o reasigna un vendedor responsable a un prospecto.
   * Si vendedorId es null, el lead vuelve a la bolsa general.
   */
  asignarVendedor(leadId: string, vendedorId: string | null): Observable<Lead> {
    return this.http.patch<Lead>(`${API_BASE}/${leadId}/asignar`, { vendedorId });
  }

}
