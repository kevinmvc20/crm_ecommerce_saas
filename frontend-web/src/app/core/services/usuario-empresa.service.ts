import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UsuarioColaborador, UsuarioCreateRequest } from '../models/usuario.models';

const API_URL = 'http://localhost:8080/api/v1/admin/usuarios';

@Injectable({
    providedIn: 'root'
})
export class UsuarioEmpresaService {
    private readonly http = inject(HttpClient);

    getUsuarios(): Observable<UsuarioColaborador[]> {
        return this.http.get<UsuarioColaborador[]>(API_URL);
    }

    crearVendedor(request: UsuarioCreateRequest): Observable<UsuarioColaborador> {
        return this.http.post<UsuarioColaborador>(API_URL, request);
    }

    toggleEstado(id: string): Observable<UsuarioColaborador> {
        return this.http.patch<UsuarioColaborador>(`${API_URL}/${id}/toggle-activo`, {});
    }
}
