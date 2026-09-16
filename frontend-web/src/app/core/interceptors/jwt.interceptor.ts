import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Interceptor funcional JWT (Angular 18 standalone).
 *
 * Responsabilidades:
 *  1. Inyecta el header `Authorization: Bearer <token>` en cada petición
 *     saliente cuando existe una sesión activa.
 *  2. Inyecta el header `X-Tenant-Id` si el usuario tiene un tenantId activo.
 *  3. Captura errores HTTP 401 (token expirado / inválido) y ejecuta logout.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();
  const tenantId = authService.currentTenant();

  // Construimos los headers adicionales solo si hay token
  let authReq = req;

  if (token) {
    let headers = req.headers.set('Authorization', `Bearer ${token}`);

    if (tenantId) {
      headers = headers.set('X-Tenant-Id', tenantId);
    }

    authReq = req.clone({ headers });
  }

  return next(authReq).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        authService.logout();
      }
      return throwError(() => error);
    })
  );
};
