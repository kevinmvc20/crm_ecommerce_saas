import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styles: ``
})
export class LoginComponent {

  // ─────────────────────────────────────────────
  // Dependencias
  // ─────────────────────────────────────────────
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // ─────────────────────────────────────────────
  // Estado de UI (Angular Signals)
  // ─────────────────────────────────────────────
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly showPassword = signal<boolean>(false);

  // ─────────────────────────────────────────────
  // Formulario fuertemente tipado
  // ─────────────────────────────────────────────
  readonly loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  // ─────────────────────────────────────────────
  // Accesores de controles
  // ─────────────────────────────────────────────
  get emailCtrl() { return this.loginForm.controls.email; }
  get passwordCtrl() { return this.loginForm.controls.password; }

  // ─────────────────────────────────────────────
  // Toggles
  // ─────────────────────────────────────────────
  togglePasswordVisibility(): void {
    this.showPassword.update(v => !v);
  }

  // ─────────────────────────────────────────────
  // Envío del formulario
  // ─────────────────────────────────────────────
  onSubmit(): void {
    // Limpia errores previos
    this.errorMessage.set(null);

    // Si el formulario es inválido, marca todos los campos como touched
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);

    const credentials = {
      email: this.emailCtrl.value as string,
      password: this.passwordCtrl.value as string
    };

    this.authService.login(credentials).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.router.navigate(['/admin/dashboard']);
      },
      error: (err: unknown) => {
        this.isLoading.set(false);

        if (err instanceof HttpErrorResponse) {
          if (err.status === 401) {
            this.errorMessage.set('Credenciales incorrectas o usuario inactivo.');
          } else if (err.status === 0) {
            this.errorMessage.set('No se puede conectar al servidor. Verifica tu conexión.');
          } else {
            this.errorMessage.set(`Error inesperado (${err.status}). Inténtalo de nuevo.`);
          }
        } else {
          this.errorMessage.set('Ocurrió un error inesperado. Por favor, inténtalo más tarde.');
        }
      }
    });
  }
}
