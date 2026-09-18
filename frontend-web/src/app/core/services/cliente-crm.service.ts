import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ClienteCRM } from '../models/crm.models';

const API_BASE = 'http://localhost:8080/api/v1/crm/clientes';

/**
 * Servicio para consumir el endpoint de Clientes del módulo CRM.
 * Utilizado principalmente para poblar el selector de clientes en el Kanban.
 */
@Injectable({ providedIn: 'root' })
export class ClienteCrmService {
  private readonly http = inject(HttpClient);

  /**
   * Lista todos los clientes activos del tenant autenticado.
   *
   * GET /api/v1/crm/clientes
   */
  getClientes(): Observable<ClienteCRM[]> {
    return this.http.get<ClienteCRM[]>(API_BASE);
  }
}
