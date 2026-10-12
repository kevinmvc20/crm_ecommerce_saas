import {
  Component, inject, signal, computed, OnInit, effect
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidatorFn
} from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { OnboardingService } from '../../../core/services/onboarding.service';
import { PlanSaaS } from '../../../core/models/tenant.models';
import { OnboardingResponse } from '../../../core/models/onboarding.models';

/** Validador: solo minúsculas, números y guiones */
const subdominioValidator: ValidatorFn = (ctrl: AbstractControl) => {
  const val: string = ctrl.value ?? '';
  return /^[a-z0-9-]*$/.test(val) ? null : { subdominioInvalido: true };
};

@Component({
  selector: 'app-onboarding-wizard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './onboarding-wizard.component.html',
  styleUrls: ['./onboarding-wizard.component.css'],
})
export class OnboardingWizardComponent implements OnInit {

  // ──────────────────────────────────────────────
  // Dependencias
  // ──────────────────────────────────────────────
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  protected readonly onboardingService = inject(OnboardingService);

  // ──────────────────────────────────────────────
  // Estado del wizard
  // ──────────────────────────────────────────────
  readonly pasoActual = signal<number>(1);
  readonly isSubmitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly planes = signal<PlanSaaS[]>([]);
  readonly planesLoading = signal<boolean>(false);
  readonly planSeleccionado = signal<PlanSaaS | null>(null);
  readonly metodoPago = signal<'TARJETA' | 'QR'>('TARJETA');
  readonly respuestaExito = signal<OnboardingResponse | null>(null);

  /** Vista previa dinámica del subdominio */
  readonly subdominioPreview = computed<string>(() => {
    const val = this.paso1Form.controls['subdominio'].value as string ?? '';
    return `${val || 'mitienda'}.tudominio.com`;
  });

  // ──────────────────────────────────────────────
  // Formulario Paso 1 — Cuenta y Empresa
  // ──────────────────────────────────────────────
  readonly paso1Form = this.fb.group({
    nombreComercial: ['', [Validators.required, Validators.minLength(2)]],
    subdominio: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30), subdominioValidator]],
    adminNombreCompleto: ['', [Validators.required, Validators.minLength(3)]],
    adminEmail: ['', [Validators.required, Validators.email]],
    adminPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  // ──────────────────────────────────────────────
  // Formulario Paso 3 — Checkout (Tarjeta)
  // ──────────────────────────────────────────────
  readonly checkoutForm = this.fb.group({
    numeroTarjeta: ['', [Validators.required, Validators.pattern(/^\d{16}$/)]],
    titular: ['', [Validators.required, Validators.minLength(3)]],
    expiracion: ['', [Validators.required, Validators.pattern(/^(0[1-9]|1[0-2])\/\d{2}$/)]],
    cvv: ['', [Validators.required, Validators.pattern(/^\d{3,4}$/)]],
  });

  ngOnInit(): void {
    this._cargarPlanes();
  }

  // ──────────────────────────────────────────────
  // Helpers de acceso a controles
  // ──────────────────────────────────────────────
  get p1() { return this.paso1Form.controls; }
  get chk() { return this.checkoutForm.controls; }

  // ──────────────────────────────────────────────
  // Navegación entre pasos
  // ──────────────────────────────────────────────
  avanzarPaso1(): void {
    this.paso1Form.markAllAsTouched();
    if (this.paso1Form.valid) {
      this.pasoActual.set(2);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  seleccionarPlan(plan: PlanSaaS): void {
    this.planSeleccionado.set(plan);
    this.pasoActual.set(3);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  retroceder(): void {
    this.pasoActual.update(p => p - 1);
    this.errorMessage.set(null);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cambiarMetodoPago(metodo: 'TARJETA' | 'QR'): void {
    this.metodoPago.set(metodo);
    this.errorMessage.set(null);
  }

  // ──────────────────────────────────────────────
  // Confirmación y envío al backend
  // ──────────────────────────────────────────────
  confirmarYActivar(): void {
    this.errorMessage.set(null);

    if (this.metodoPago() === 'TARJETA') {
      this.checkoutForm.markAllAsTouched();
      if (this.checkoutForm.invalid) return;
    }

    this.isSubmitting.set(true);

    const p1 = this.paso1Form.getRawValue();
    const plan = this.planSeleccionado()!;
    const chk = this.checkoutForm.getRawValue();
    const metodo = this.metodoPago();

    const payload = {
      nombreComercial: p1.nombreComercial!,
      subdominio: p1.subdominio!,
      planSaaSId: plan.id,
      adminNombreCompleto: p1.adminNombreCompleto!,
      adminEmail: p1.adminEmail!,
      adminPassword: p1.adminPassword!,
      metodoPago: metodo,
      ...(metodo === 'TARJETA' ? {
        numeroTarjeta: chk.numeroTarjeta!,
        titular: chk.titular!,
        expiracion: chk.expiracion!,
        cvv: chk.cvv!,
      } : {}),
    };

    this.onboardingService.registrarEmpresa(payload).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.respuestaExito.set(res);
        this.pasoActual.set(4);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (err: unknown) => {
        this.isSubmitting.set(false);
        if (err instanceof HttpErrorResponse) {
          const body = err.error;
          // Manejo de RFC 9457 Problem Detail
          if (body?.detail) {
            this.errorMessage.set(body.detail);
          } else if (body?.message) {
            this.errorMessage.set(body.message);
          } else if (err.status === 409) {
            this.errorMessage.set('El subdominio o el email ya están registrados. Por favor elige otros.');
          } else if (err.status === 400) {
            this.errorMessage.set('Datos inválidos. Revisa los campos e inténtalo de nuevo.');
          } else if (err.status === 0) {
            this.errorMessage.set('No se puede conectar al servidor. Verifica tu conexión.');
          } else {
            this.errorMessage.set(`Error inesperado (${err.status}). Inténtalo más tarde.`);
          }
        } else {
          this.errorMessage.set('Ocurrió un error inesperado. Inténtalo más tarde.');
        }
      }
    });
  }

  simularEscaneoQR(): void {
    this.confirmarYActivar();
  }

  irAlLogin(): void {
    this.router.navigate(['/login']);
  }

  // ──────────────────────────────────────────────
  // Helpers de plantilla
  // ──────────────────────────────────────────────
  formatearPrecio(precio: number): string {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(precio);
  }

  formatearLimite(limite: number): string {
    return limite === -1 ? 'Ilimitado' : limite.toString();
  }

  // ──────────────────────────────────────────────
  // Carga de planes
  // ──────────────────────────────────────────────
  private _cargarPlanes(): void {
    this.planesLoading.set(true);
    this.onboardingService.getPlanesPublicos().subscribe({
      next: (planes) => {
        this.planes.set(planes);
        this.planesLoading.set(false);
      },
      error: () => {
        this.planesLoading.set(false);
      }
    });
  }
}
