import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  AbstractControl,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { finalize } from 'rxjs';
import { LeadService } from '../../../../core/services/lead.service';
import { TenantService } from '../../../../core/services/tenant.service';
import { AuthService } from '../../../../core/services/auth.service';
import { UsuarioTenant } from '../../../../core/models/tenant.models';
import {
  EstadoLead,
  Lead,
  LeadCalificarRequest,
  LeadConvertirRequest,
  LeadCreateRequest,
} from '../../../../core/models/crm.models';

// ─── Validador de grupo: al menos email o teléfono ───────────────────────────

/**
 * ValidatorFn a nivel de FormGroup.
 * Verifica que `email` o `telefono` tengan valor no vacío.
 * Si ambos están vacíos, agrega el error `atLeastOneContact` al grupo.
 */
const atLeastOneContactValidator: ValidatorFn = (
  group: AbstractControl
): ValidationErrors | null => {
  const email = (group.get('email')?.value ?? '').toString().trim();
  const telefono = (group.get('telefono')?.value ?? '').toString().trim();
  return email.length > 0 || telefono.length > 0
    ? null
    : { atLeastOneContact: true };
};

@Component({
  selector: 'app-lead-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './lead-list.component.html',
})
export class LeadListComponent implements OnInit {

  // ─────────────────────────────────────────────────────
  // Dependencias
  // ─────────────────────────────────────────────────────
  private readonly leadService = inject(LeadService);
  private readonly tenantService = inject(TenantService);
  protected readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  // ─────────────────────────────────────────────────────
  // Estado reactivo principal
  // ─────────────────────────────────────────────────────
  readonly leads = signal<Lead[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string | null>(null);

  // ─────────────────────────────────────────────────────
  // Vendedores disponibles para el selector
  // ─────────────────────────────────────────────────────
  readonly vendedores = signal<UsuarioTenant[]>([]);

  // ─────────────────────────────────────────────────────
  // Modal: Nuevo Prospecto
  // ─────────────────────────────────────────────────────
  readonly isNuevoModalOpen = signal<boolean>(false);
  readonly isSubmittingNuevo = signal<boolean>(false);
  readonly nuevoError = signal<string | null>(null);

  /**
   * Formulario de creación de lead.
   * - nombre: requerido, mínimo 3 caracteres.
   * - email: opcional pero con validación de formato si se ingresa.
   * - telefono: opcional.
   * - vendedorId: opcional.
   * Validador de grupo: al menos email o telefono deben tener valor.
   */
  readonly nuevoForm: FormGroup = this.fb.group(
    {
      nombre: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.email]],
      telefono: [''],
      vendedorId: [null],
    },
    { validators: atLeastOneContactValidator }
  );

  // ─────────────────────────────────────────────────────
  // Modal: Calificar Lead
  // ─────────────────────────────────────────────────────
  readonly isCalificarModalOpen = signal<boolean>(false);
  readonly isSubmittingCalificar = signal<boolean>(false);
  readonly calificarError = signal<string | null>(null);
  readonly leadACalificar = signal<Lead | null>(null);

  readonly calificarForm: FormGroup = this.fb.group({
    score: [null, [Validators.required, Validators.min(0), Validators.max(100)]],
    notas: ['', [Validators.required, Validators.minLength(5)]],
  });

  // ─────────────────────────────────────────────────────
  // Modal: Convertir a Cliente
  // ─────────────────────────────────────────────────────
  readonly isConvertirModalOpen = signal<boolean>(false);
  readonly isSubmittingConvertir = signal<boolean>(false);
  readonly convertirError = signal<string | null>(null);
  readonly leadAConvertir = signal<Lead | null>(null);

  /**
   * Formulario de conversión a cliente.
   * - ciNit: requerido, mínimo 4 caracteres (actualizado según requisitos).
   * - razonSocialONombre: requerido, mínimo 3 caracteres (actualizado).
   */
  readonly convertirForm: FormGroup = this.fb.group({
    ciNit: ['', [Validators.required, Validators.minLength(4)]],
    razonSocialONombre: ['', [Validators.required, Validators.minLength(3)]],
  });



  // ─────────────────────────────────────────────────────
  // Modal: Asignar Vendedor (ADMIN_EMPRESA)
  // ─────────────────────────────────────────────────────
  readonly isAsignarModalOpen = signal<boolean>(false);
  readonly isSubmittingAsignar = signal<boolean>(false);
  readonly asignarError = signal<string | null>(null);
  readonly leadParaAsignar = signal<Lead | null>(null);

  readonly asignarForm: FormGroup = this.fb.group({
    vendedorId: [null],
  });

  // ─────────────────────────────────────────────────────
  // Getter: error de contacto del grupo nuevoForm
  // ─────────────────────────────────────────────────────

  /**
   * true cuando el grupo tiene el error `atLeastOneContact` y el usuario
   * ya tocó al menos uno de los dos campos de contacto.
   * Implementado como getter para que Angular CD lo evalúe en cada ciclo.
   */
  get showContactError(): boolean {
    const emailTouched = this.nuevoForm.get('email')?.touched ?? false;
    const telefonoTouched = this.nuevoForm.get('telefono')?.touched ?? false;
    return (
      this.nuevoForm.hasError('atLeastOneContact') &&
      (emailTouched || telefonoTouched)
    );
  }

  // ─────────────────────────────────────────────────────
  // Ciclo de vida
  // ─────────────────────────────────────────────────────
  ngOnInit(): void {
    this.cargarLeads();
    this.cargarVendedores();
  }

  // ─────────────────────────────────────────────────────
  // Carga de datos
  // ─────────────────────────────────────────────────────

  /** Carga la lista de leads desde el backend */
  cargarLeads(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.leadService
      .getLeads()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (data) => this.leads.set(data),
        error: (err) => {
          const msg = err?.error?.message ?? 'No se pudieron cargar los prospectos.';
          this.errorMessage.set(msg);
        },
      });
  }

  /**
   * Carga los usuarios del tenant actual y filtra aquellos con rol
   * ROLE_VENDEDOR o ROLE_ADMIN_EMPRESA para el selector de responsable.
   */
  cargarVendedores(): void {
    const tenantId = this.authService.currentTenant();
    if (!tenantId) return;

    this.tenantService.getUsuariosByTenant(tenantId).subscribe({
      next: (usuarios) => {
        const rolesPermitidos = new Set(['ROLE_VENDEDOR', 'ROLE_ADMIN_EMPRESA']);
        this.vendedores.set(
          usuarios.filter(
            (u) => rolesPermitidos.has(u.rolNombre) && u.activo
          )
        );
      },
      // Silencioso: si falla, el selector aparece vacío (no bloquea el flujo).
      error: () => this.vendedores.set([]),
    });
  }

  // ─────────────────────────────────────────────────────
  // Modal: Nuevo Prospecto
  // ─────────────────────────────────────────────────────

  abrirNuevoModal(): void {
    // 1. Resetear con valores por defecto; primero habilitar todos los controles
    //    para que reset() funcione correctamente aunque estuviese disabled.
    this.nuevoForm.get('vendedorId')?.enable();
    this.nuevoForm.reset({ nombre: '', email: '', telefono: '', vendedorId: null });
    this.nuevoError.set(null);

    // 2. Auto-asignación según rol del usuario en sesión
    const user = this.authService.currentUser();

    if (user?.rol === 'ROLE_VENDEDOR') {
      // Buscar el usuario en la lista de vendedores por email
      const propio = this.vendedores().find((v) => v.email === user.email);
      if (propio) {
        this.nuevoForm.get('vendedorId')?.setValue(propio.id);
        // Bloquear el selector: el vendedor no puede reasignarse
        this.nuevoForm.get('vendedorId')?.disable();
      }
    }
    // ROLE_ADMIN_EMPRESA: control habilitado, valor null por defecto (ya hecho en reset)

    this.isNuevoModalOpen.set(true);
  }

  cerrarNuevoModal(): void {
    this.isNuevoModalOpen.set(false);
  }

  onSubmitNuevo(): void {
    if (this.nuevoForm.invalid) {
      this.nuevoForm.markAllAsTouched();
      return;
    }

    // getRawValue() captura también los controles deshabilitados (vendedorId
    // se deshabilita para ROLE_VENDEDOR, pero su valor debe incluirse en el payload).
    const raw = this.nuevoForm.getRawValue();
    const payload: LeadCreateRequest = {
      nombre: raw.nombre,
      email: raw.email || undefined,
      telefono: raw.telefono || undefined,
    };
    if (raw.vendedorId) {
      payload.vendedorId = raw.vendedorId;
    }

    this.isSubmittingNuevo.set(true);
    this.nuevoError.set(null);

    this.leadService
      .crearLead(payload)
      .pipe(finalize(() => this.isSubmittingNuevo.set(false)))
      .subscribe({
        next: (nuevo) => {
          this.leads.update((list) => [nuevo, ...list]);
          this.cerrarNuevoModal();
        },
        error: (err) => {
          this.nuevoError.set(
            err?.error?.message ?? 'Error al crear el prospecto.'
          );
        },
      });
  }

  // ─────────────────────────────────────────────────────
  // Modal: Calificar Lead
  // ─────────────────────────────────────────────────────

  abrirCalificarModal(lead: Lead): void {
    this.leadACalificar.set(lead);
    this.calificarForm.reset({ score: lead.score ?? null, notas: '' });
    this.calificarError.set(null);
    this.isCalificarModalOpen.set(true);
  }

  cerrarCalificarModal(): void {
    this.isCalificarModalOpen.set(false);
    this.leadACalificar.set(null);
  }

  onSubmitCalificar(): void {
    if (this.calificarForm.invalid) {
      this.calificarForm.markAllAsTouched();
      return;
    }

    const lead = this.leadACalificar();
    if (!lead) return;

    const payload: LeadCalificarRequest = this.calificarForm.value;
    this.isSubmittingCalificar.set(true);
    this.calificarError.set(null);

    this.leadService
      .calificarLead(lead.id, payload)
      .pipe(finalize(() => this.isSubmittingCalificar.set(false)))
      .subscribe({
        next: (updated) => {
          this.leads.update((list) =>
            list.map((l) => (l.id === updated.id ? updated : l))
          );
          this.cerrarCalificarModal();
        },
        error: (err) => {
          this.calificarError.set(
            err?.error?.message ?? 'Error al calificar el prospecto.'
          );
        },
      });
  }

  // ─────────────────────────────────────────────────────
  // Modal: Convertir a Cliente
  // ─────────────────────────────────────────────────────

  abrirConvertirModal(lead: Lead): void {
    this.leadAConvertir.set(lead);
    this.convertirForm.reset({ ciNit: '', razonSocialONombre: '' });
    this.convertirError.set(null);
    this.isConvertirModalOpen.set(true);
  }

  cerrarConvertirModal(): void {
    this.isConvertirModalOpen.set(false);
    this.leadAConvertir.set(null);
  }

  onSubmitConvertir(): void {
    if (this.convertirForm.invalid) {
      this.convertirForm.markAllAsTouched();
      return;
    }

    const lead = this.leadAConvertir();
    if (!lead) return;

    const payload: LeadConvertirRequest = this.convertirForm.value;
    this.isSubmittingConvertir.set(true);
    this.convertirError.set(null);

    this.leadService
      .convertirACliente(lead.id, payload)
      .pipe(finalize(() => this.isSubmittingConvertir.set(false)))
      .subscribe({
        next: (updated) => {
          this.leads.update((list) =>
            list.map((l) => (l.id === updated.id ? updated : l))
          );
          this.cerrarConvertirModal();
        },
        error: (err) => {
          this.convertirError.set(
            err?.error?.message ?? 'Error al convertir el prospecto a cliente.'
          );
        },
      });
  }

  // ─────────────────────────────────────────────────────
  // Modal: Asignar Vendedor
  // ─────────────────────────────────────────────────────

  abrirAsignarModal(lead: Lead): void {
    this.leadParaAsignar.set(lead);
    this.asignarForm.reset({ vendedorId: lead.vendedorId ?? null });
    this.asignarError.set(null);
    this.isAsignarModalOpen.set(true);
  }

  cerrarAsignarModal(): void {
    this.isAsignarModalOpen.set(false);
    this.leadParaAsignar.set(null);
  }

  onSubmitAsignar(): void {
    const lead = this.leadParaAsignar();
    if (!lead) return;

    const { vendedorId } = this.asignarForm.value;
    this.isSubmittingAsignar.set(true);
    this.asignarError.set(null);

    this.leadService
      .asignarVendedor(lead.id, vendedorId)
      .pipe(finalize(() => this.isSubmittingAsignar.set(false)))
      .subscribe({
        next: (updated) => {
          this.leads.update((list) =>
            list.map((l) => (l.id === updated.id ? updated : l))
          );
          this.cerrarAsignarModal();
        },
        error: (err) => {
          this.asignarError.set(
            err?.error?.message ?? 'Error al asignar el vendedor.'
          );
        },
      });
  }

  // ─────────────────────────────────────────────────────
  // Helpers de plantilla
  // ─────────────────────────────────────────────────────

  /** Clases Tailwind del badge según estado del lead */
  estadoBadgeClass(estado: EstadoLead): string {
    const map: Record<EstadoLead, string> = {
      NUEVO: 'bg-sky-100 text-sky-700 ring-sky-300',
      CONTACTADO: 'bg-violet-100 text-violet-700 ring-violet-300',
      CALIFICADO: 'bg-emerald-100 text-emerald-700 ring-emerald-300',
      DESCALIFICADO: 'bg-red-100 text-red-700 ring-red-300',
      CONVERTIDO: 'bg-amber-100 text-amber-700 ring-amber-300',
    };
    return map[estado] ?? 'bg-slate-100 text-slate-600 ring-slate-300';
  }

  /** Etiqueta legible del estado */
  estadoLabel(estado: EstadoLead): string {
    const map: Record<EstadoLead, string> = {
      NUEVO: 'Nuevo',
      CONTACTADO: 'Contactado',
      CALIFICADO: 'Calificado',
      DESCALIFICADO: 'Descalificado',
      CONVERTIDO: 'Convertido',
    };
    return map[estado] ?? estado;
  }

  /** Color de la barra de progreso del score */
  scoreBarClass(score: number | null): string {
    if (score === null) return 'bg-slate-300';
    if (score >= 70) return 'bg-emerald-500';
    if (score >= 40) return 'bg-amber-400';
    return 'bg-red-400';
  }

  /** Etiqueta legible del rol de vendedor para el selector */
  rolVendedorLabel(rol: string): string {
    return rol === 'ROLE_ADMIN_EMPRESA' ? 'Admin' : 'Vendedor';
  }

  /** Verifica si un control del nuevoForm tiene error visible */
  hasNuevoError(control: string, error: string): boolean {
    const c = this.nuevoForm.get(control);
    return !!(c && c.touched && c.hasError(error));
  }

  /**
   * Devuelve true si el campo de contacto debe mostrar borde rojo.
   * Condición: campo tocado Y el grupo tiene el error atLeastOneContact.
   */
  contactFieldInvalid(control: string): boolean {
    const c = this.nuevoForm.get(control);
    return !!(
      c?.touched &&
      this.nuevoForm.hasError('atLeastOneContact')
    );
  }

  /** Verifica si un control del calificarForm tiene error visible */
  hasCalificarError(control: string, error: string): boolean {
    const c = this.calificarForm.get(control);
    return !!(c && c.touched && c.hasError(error));
  }

  /** Verifica si un control del convertirForm tiene error visible */
  hasConvertirError(control: string, error: string): boolean {
    const c = this.convertirForm.get(control);
    return !!(c && c.touched && c.hasError(error));
  }

  /**
   * true si el campo del convertirForm es inválido y fue tocado.
   * Usado para clases Tailwind condicionales (border-red-500, ring-red-500).
   */
  convertirFieldInvalid(control: string): boolean {
    const c = this.convertirForm.get(control);
    return !!(c && c.invalid && c.touched);
  }

  /**
   * Controla si un lead puede ser calificado.
   * Requiere vendedor asignado + estado activo (no CONVERTIDO ni DESCALIFICADO).
   */
  puedeCalificar(lead: Lead): boolean {
    return (
      lead.vendedorId !== null &&
      lead.vendedorId !== undefined &&
      lead.estado !== 'CONVERTIDO' &&
      lead.estado !== 'DESCALIFICADO'
    );
  }

  /**
   * Controla si un lead puede ser convertido a cliente.
   * Requiere vendedor asignado + estado CALIFICADO + score >= 70.
   */
  puedeConvertir(lead: Lead): boolean {
    return (
      lead.vendedorId !== null &&
      lead.vendedorId !== undefined &&
      lead.estado === 'CALIFICADO' &&
      (lead.score ?? 0) >= 70
    );
  }

  /**
   * true cuando el lead está CALIFICADO con vendedor pero su score
   * es insuficiente (< 70) para la conversión a cliente.
   * Se usa para mostrar el pill informativo en la columna de acciones.
   */
  scoreInsuficienteParaConversion(lead: Lead): boolean {
    return (
      lead.vendedorId !== null &&
      lead.vendedorId !== undefined &&
      lead.estado === 'CALIFICADO' &&
      (lead.score ?? 0) < 70
    );
  }

  /**
   * Devuelve la etiqueta y las clases Tailwind del badge de temperatura
   * comercial según el score del lead. Se usa en el modal de calificación.
   */
  temperaturaScore(score: number | null): { label: string; classBadge: string } {
    const s = score ?? 0;
    if (s >= 70) {
      return {
        label: 'Apto para conversión (Caliente)',
        classBadge: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
      };
    }
    if (s >= 40) {
      return {
        label: 'En maduración (Tibio)',
        classBadge: 'bg-amber-50 text-amber-700 ring-amber-200',
      };
    }
    return {
      label: 'Interés preliminar (Frío)',
      classBadge: 'bg-red-50 text-red-700 ring-red-200',
    };
  }

  /**
   * Indica que el lead necesita asignación de vendedor para avanzar
   * en el pipeline (está en la Bolsa General y sigue activo).
   */
  requiereAsignacion(lead: Lead): boolean {
    return (
      (lead.vendedorId === null || lead.vendedorId === undefined) &&
      lead.estado !== 'CONVERTIDO' &&
      lead.estado !== 'DESCALIFICADO'
    );
  }

  /** Permite asignar/reasignar si es ADMIN_EMPRESA y el lead está activo */
  puedeAsignar(lead: Lead): boolean {
    return (
      this.authService.currentUser()?.rol === 'ROLE_ADMIN_EMPRESA' &&
      lead.estado !== 'CONVERTIDO' &&
      lead.estado !== 'DESCALIFICADO'
    );
  }
}
