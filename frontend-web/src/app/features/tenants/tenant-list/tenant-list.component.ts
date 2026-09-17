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
import {
  PlanSaaS,
  Tenant,
  TenantCreateRequest,
  UsuarioTenant,
  UsuarioTenantCreateRequest,
} from '../../../core/models/tenant.models';
import { finalize } from 'rxjs';

/** Formulario tipado para crear un tenant */
interface TenantForm {
  nombreComercial: string;
  subdominio: string;
  planSaaSId: number | null;
}

/** Formulario tipado para crear un usuario de tenant */
interface UsuarioTenantForm {
  nombreCompleto: string;
  email: string;
  password: string;
  rolNombre: string;
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
  // Estado reactivo — listado principal (Angular Signals)
  // ─────────────────────────────────────────────────────
  readonly tenants      = signal<Tenant[]>([]);
  readonly planes       = signal<PlanSaaS[]>([]);
  readonly isLoading    = signal<boolean>(true);
  readonly isModalOpen  = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  // ─────────────────────────────────────────────────────
  // Estado reactivo — modal detalles / usuarios de tenant
  // ─────────────────────────────────────────────────────
  readonly selectedTenant     = signal<Tenant | null>(null);
  readonly tenantUsuarios      = signal<UsuarioTenant[]>([]);
  readonly isLoadingUsuarios   = signal<boolean>(false);
  readonly isUserModalOpen     = signal<boolean>(false);
  readonly isCreatingUser      = signal<boolean>(false);
  readonly userErrorMessage    = signal<string | null>(null);
  readonly userSuccessMessage  = signal<string | null>(null);

  // ─────────────────────────────────────────────────────
  // Formulario reactivo — Crear tenant
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
  // Formulario reactivo tipado — Crear usuario de tenant
  // ─────────────────────────────────────────────────────
  readonly usuarioForm: FormGroup = this.fb.group({
    nombreCompleto: ['', [Validators.required]],
    email:          ['', [Validators.required, Validators.email]],
    password:       ['', [Validators.required, Validators.minLength(6)]],
    rolNombre:      ['ROLE_ADMIN_EMPRESA', [Validators.required]],
  });

  // ─────────────────────────────────────────────────────
  // Ciclo de vida
  // ─────────────────────────────────────────────────────
  ngOnInit(): void {
    this.cargarDatos();
  }

  // ─────────────────────────────────────────────────────
  // Métodos públicos — listado principal
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

  /** Abre el modal de nuevo tenant y reinicia el formulario */
  abrirModal(): void {
    this.tenantForm.reset({ nombreComercial: '', subdominio: '', planSaaSId: null });
    this.errorMessage.set(null);
    this.isModalOpen.set(true);
  }

  /** Cierra el modal de nuevo tenant */
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
  // Métodos públicos — modal detalles / usuarios de tenant
  // ─────────────────────────────────────────────────────

  /**
   * Abre el drawer/modal de detalles de una empresa, setea el tenant
   * seleccionado y carga su lista de usuarios vía la API.
   */
  abrirDetallesTenant(tenant: Tenant): void {
    this.selectedTenant.set(tenant);
    this.tenantUsuarios.set([]);
    this.userErrorMessage.set(null);
    this.userSuccessMessage.set(null);
    this.usuarioForm.reset({ nombreCompleto: '', email: '', password: '', rolNombre: 'ROLE_ADMIN_EMPRESA' });
    this.isUserModalOpen.set(true);

    this.isLoadingUsuarios.set(true);
    this.tenantService
      .getUsuariosByTenant(tenant.id)
      .pipe(finalize(() => this.isLoadingUsuarios.set(false)))
      .subscribe({
        next: (usuarios) => this.tenantUsuarios.set(usuarios),
        error: () => this.userErrorMessage.set('No se pudieron cargar los usuarios de esta empresa.'),
      });
  }

  /** Cierra el drawer/modal de detalles */
  cerrarDetallesTenant(): void {
    this.isUserModalOpen.set(false);
    this.selectedTenant.set(null);
  }

  /**
   * Envía el formulario de creación de usuario para el tenant seleccionado.
   * Refresca la lista de usuarios del tenant tras el alta exitosa.
   */
  onSubmitCrearUsuario(): void {
    if (this.usuarioForm.invalid) {
      this.usuarioForm.markAllAsTouched();
      return;
    }

    const tenant = this.selectedTenant();
    if (!tenant) return;

    const raw = this.usuarioForm.value as UsuarioTenantForm;
    const payload: UsuarioTenantCreateRequest = {
      nombreCompleto: raw.nombreCompleto,
      email: raw.email,
      password: raw.password,
      rolNombre: raw.rolNombre,
    };

    this.isCreatingUser.set(true);
    this.userErrorMessage.set(null);
    this.userSuccessMessage.set(null);

    this.tenantService
      .crearUsuarioTenant(tenant.id, payload)
      .pipe(finalize(() => this.isCreatingUser.set(false)))
      .subscribe({
        next: (newUser) => {
          // Actualiza la lista inmediatamente sin recargar toda la lista
          this.tenantUsuarios.update((list) => [...list, newUser]);
          this.userSuccessMessage.set(`Usuario "${newUser.nombreCompleto}" creado correctamente.`);
          this.usuarioForm.reset({ nombreCompleto: '', email: '', password: '', rolNombre: 'ROLE_ADMIN_EMPRESA' });
        },
        error: (err) => {
          const msg =
            err?.error?.message ?? 'Error al crear el usuario. Verifica los datos e inténtalo de nuevo.';
          this.userErrorMessage.set(msg);
        },
      });
  }

  // ─────────────────────────────────────────────────────
  // Helpers de plantilla
  // ─────────────────────────────────────────────────────

  /** Devuelve las clases CSS del badge según el estado de suscripción */
  estadoBadgeClass(estado: string): string {
    const map: Record<string, string> = {
      ACTIVO:     'bg-emerald-100 text-emerald-700 ring-emerald-300',
      TRIAL:      'bg-blue-100 text-blue-700 ring-blue-300',
      SUSPENDIDO: 'bg-amber-100 text-amber-700 ring-amber-300',
      CANCELADO:  'bg-red-100 text-red-700 ring-red-300',
    };
    return map[estado.toUpperCase()] ?? 'bg-slate-100 text-slate-600 ring-slate-300';
  }

  /** Devuelve las clases CSS del badge según el rol del usuario */
  rolBadgeClass(rol: string): string {
    return rol === 'ROLE_ADMIN_EMPRESA'
      ? 'bg-violet-100 text-violet-700 ring-violet-300'
      : 'bg-sky-100 text-sky-700 ring-sky-300';
  }

  /** Etiqueta legible para el rol */
  rolLabel(rol: string): string {
    const map: Record<string, string> = {
      ROLE_ADMIN_EMPRESA: 'Admin Empresa',
      ROLE_VENDEDOR:      'Vendedor',
      ROLE_SUPER_ADMIN:   'Super Admin',
    };
    return map[rol] ?? rol;
  }

  /** Verifica si un control del tenantForm tiene error visible */
  hasError(control: string, error: string): boolean {
    const c = this.tenantForm.get(control);
    return !!(c && c.touched && c.hasError(error));
  }

  /** Verifica si un control del usuarioForm tiene error visible */
  hasUserError(control: string, error: string): boolean {
    const c = this.usuarioForm.get(control);
    return !!(c && c.touched && c.hasError(error));
  }
}
