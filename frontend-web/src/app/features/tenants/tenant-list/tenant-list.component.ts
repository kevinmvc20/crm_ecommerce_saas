import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TenantService } from '../../../core/services/tenant.service';
import { PlanSaaS, Tenant, TenantCreateRequest } from '../../../core/models/tenant.models';
import { finalize } from 'rxjs';

/** Formulario tipado para crear un tenant */
interface TenantForm {
  nombreComercial: string;
  subdominio: string;
  planSaaSId: number | null;
}

@Component({
  selector: 'app-tenant-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './tenant-list.component.html',
})
export class TenantListComponent implements OnInit {

  // ─────────────────────────────────────────────────────
  // Dependencias
  // ─────────────────────────────────────────────────────
  private readonly tenantService = inject(TenantService);
  private readonly fb = inject(FormBuilder);

  // ─────────────────────────────────────────────────────
  // Estado reactivo (Angular Signals)
  // ─────────────────────────────────────────────────────
  readonly tenants    = signal<Tenant[]>([]);
  readonly planes     = signal<PlanSaaS[]>([]);
  readonly isLoading  = signal<boolean>(true);
  readonly isModalOpen = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  // ─────────────────────────────────────────────────────
  // Formulario reactivo
  // ─────────────────────────────────────────────────────
  readonly tenantForm: FormGroup = this.fb.group({
    nombreComercial: ['', [Validators.required, Validators.minLength(2)]],
    subdominio: [
      '',
      [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(63),
        // Solo minúsculas, números y guiones; sin espacios ni caracteres especiales
        Validators.pattern(/^[a-z0-9][a-z0-9-]*[a-z0-9]$|^[a-z0-9]$/),
      ],
    ],
    planSaaSId: [null, [Validators.required]],
  });

  // ─────────────────────────────────────────────────────
  // Ciclo de vida
  // ─────────────────────────────────────────────────────
  ngOnInit(): void {
    this.cargarDatos();
  }

  // ─────────────────────────────────────────────────────
  // Métodos públicos
  // ─────────────────────────────────────────────────────

  /** Carga en paralelo la lista de tenants y planes SaaS */
  cargarDatos(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    // Carga planes disponibles
    this.tenantService.getPlanes().subscribe({
      next: (data) => this.planes.set(data),
      error: () => this.errorMessage.set('No se pudieron cargar los planes SaaS.'),
    });

    // Carga lista de tenants
    this.tenantService
      .getTenants()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (data) => this.tenants.set(data),
        error: () => this.errorMessage.set('No se pudieron cargar las empresas registradas.'),
      });
  }

  /** Abre el modal y reinicia el formulario */
  abrirModal(): void {
    this.tenantForm.reset({ nombreComercial: '', subdominio: '', planSaaSId: null });
    this.errorMessage.set(null);
    this.isModalOpen.set(true);
  }

  /** Cierra el modal */
  cerrarModal(): void {
    this.isModalOpen.set(false);
  }

  /** Envía el formulario y crea el tenant */
  onSubmit(): void {
    if (this.tenantForm.invalid) {
      this.tenantForm.markAllAsTouched();
      return;
    }

    const raw = this.tenantForm.value as TenantForm;
    const payload: TenantCreateRequest = {
      nombreComercial: raw.nombreComercial,
      subdominio: raw.subdominio,
      planSaaSId: Number(raw.planSaaSId),
    };

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    this.tenantService
      .createTenant(payload)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (newTenant) => {
          this.tenants.update((list) => [...list, newTenant]);
          this.cerrarModal();
        },
        error: (err) => {
          const msg =
            err?.error?.message ?? 'Error al crear la empresa. Inténtalo de nuevo.';
          this.errorMessage.set(msg);
        },
      });
  }

  // ─────────────────────────────────────────────────────
  // Helpers de plantilla
  // ─────────────────────────────────────────────────────

  /** Devuelve las clases CSS del badge según el estado de suscripción */
  estadoBadgeClass(estado: string): string {
    const map: Record<string, string> = {
      ACTIVO:   'bg-emerald-100 text-emerald-700 ring-emerald-300',
      TRIAL:    'bg-blue-100 text-blue-700 ring-blue-300',
      SUSPENDIDO: 'bg-amber-100 text-amber-700 ring-amber-300',
      CANCELADO:  'bg-red-100 text-red-700 ring-red-300',
    };
    return map[estado.toUpperCase()] ?? 'bg-slate-100 text-slate-600 ring-slate-300';
  }

  /** Verifica si un control tiene error visible */
  hasError(control: string, error: string): boolean {
    const c = this.tenantForm.get(control);
    return !!(c && c.touched && c.hasError(error));
  }
}
