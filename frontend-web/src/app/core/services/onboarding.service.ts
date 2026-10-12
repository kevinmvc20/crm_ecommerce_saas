import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PlanSaaS } from '../models/tenant.models';
import { OnboardingRequest, OnboardingResponse } from '../models/onboarding.models';

const API_BASE = 'http://localhost:8080/api/v1/public/onboarding';

/**
 * Servicio para el flujo público de registro Self-Service (Onboarding).
 *
 * Estos endpoints son completamente públicos y NO requieren JWT ni
 * el interceptor de autenticación.
 */
@Injectable({ providedIn: 'root' })
export class OnboardingService {
  private readonly http = inject(HttpClient);

  /**
   * Obtiene la lista de planes SaaS disponibles para el registro público.
   * Endpoint: GET /api/v1/public/onboarding/planes
   */
  getPlanesPublicos(): Observable<PlanSaaS[]> {
    return this.http.get<PlanSaaS[]>(`${API_BASE}/planes`);
  }

  /**
   * Registra una nueva empresa a través del flujo de onboarding público.
   * Endpoint: POST /api/v1/public/onboarding/register
   */
  registrarEmpresa(req: OnboardingRequest): Observable<OnboardingResponse> {
    return this.http.post<OnboardingResponse>(`${API_BASE}/register`, req);
  }
}
