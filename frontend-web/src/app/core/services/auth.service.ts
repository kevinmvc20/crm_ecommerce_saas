import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, UserSession } from '../models/auth.models';

const API_URL = 'http://localhost:8080/api/v1/auth';
const TOKEN_KEY = 'auth_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  // ──────────────────────────────────────────
  // Estado reactivo mediante Angular Signals
  // ──────────────────────────────────────────
  private readonly _currentUser = signal<UserSession | null>(null);
  private readonly _token = signal<string | null>(null);

  /** Sesión del usuario actualmente autenticado */
  readonly currentUser = this._currentUser.asReadonly();

  /** true si hay una sesión activa */
  readonly isAuthenticated = computed(() => this._currentUser() !== null);

  /** tenantId del usuario activo (null para SUPER_ADMIN) */
  readonly currentTenant = computed(() => this._currentUser()?.tenantId ?? null);

  constructor() {
    // Restaura la sesión desde localStorage al iniciar la app
    this._restoreSession();
  }

  // ──────────────────────────────────────────
  // Métodos públicos
  // ──────────────────────────────────────────

  /**
   * Autentica al usuario contra el backend.
   * Almacena el token y actualiza el estado de sesión.
   */
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_URL}/login`, credentials)
      .pipe(tap((response) => this._handleAuthSuccess(response)));
  }

  /**
   * Cierra la sesión: limpia storage, reinicia Signals y redirige a /login.
   */
  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this._token.set(null);
    this._currentUser.set(null);
    this.router.navigate(['/login']);
  }

  /**
   * Devuelve el JWT almacenado o null si no hay sesión.
   */
  getToken(): string | null {
    return this._token();
  }

  /**
   * Verifica si el usuario tiene el rol indicado.
   */
  hasRole(role: string): boolean {
    return this._currentUser()?.rol === role;
  }

  // ──────────────────────────────────────────
  // Métodos privados
  // ──────────────────────────────────────────

  private _handleAuthSuccess(response: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, response.token);
    this._token.set(response.token);
    this._currentUser.set({
      email: response.email,
      rol: response.rol,
      tenantId: response.tenantId,
      nombreTenant: response.nombreTenant,
    });
  }

  private _restoreSession(): void {
    const storedToken = localStorage.getItem(TOKEN_KEY);
    if (storedToken) {
      // Solo restauramos el token; la sesión completa se reconstruiría
      // en un endpoint /me si se dispusiera de él.
      this._token.set(storedToken);
    }
  }
}
