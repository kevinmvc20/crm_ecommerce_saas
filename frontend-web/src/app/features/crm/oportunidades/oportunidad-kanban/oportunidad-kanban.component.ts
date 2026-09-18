import {
  Component,
  inject,
  signal,
  computed,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { finalize } from 'rxjs';

import { OportunidadService } from '../../../../core/services/oportunidad.service';
import { ClienteCrmService } from '../../../../core/services/cliente-crm.service';
import {
  ClienteCRM,
  EtapaOportunidad,
  Oportunidad,
  OportunidadCreateRequest,
} from '../../../../core/models/crm.models';

// ─── Metadata de columnas Kanban ─────────────────────────────────────────────

interface KanbanColumn {
  etapa: EtapaOportunidad;
  label: string;
  colorDot: string;
  colorHeader: string;
  colorBadge: string;
  colorBorder: string;
  colorBg: string;
  isTerminal: boolean;
}

const KANBAN_COLUMNS: KanbanColumn[] = [
  {
    etapa: 'CALIFICACION',
    label: 'Calificación',
    colorDot: 'bg-sky-400',
    colorHeader: 'text-sky-700',
    colorBadge: 'bg-sky-100 text-sky-700 ring-sky-200',
    colorBorder: 'border-sky-200',
    colorBg: 'bg-sky-50/50',
    isTerminal: false,
  },
  {
    etapa: 'PROPUESTA',
    label: 'Propuesta',
    colorDot: 'bg-violet-400',
    colorHeader: 'text-violet-700',
    colorBadge: 'bg-violet-100 text-violet-700 ring-violet-200',
    colorBorder: 'border-violet-200',
    colorBg: 'bg-violet-50/50',
    isTerminal: false,
  },
  {
    etapa: 'NEGOCIACION',
    label: 'Negociación',
    colorDot: 'bg-amber-400',
    colorHeader: 'text-amber-700',
    colorBadge: 'bg-amber-100 text-amber-700 ring-amber-200',
    colorBorder: 'border-amber-200',
    colorBg: 'bg-amber-50/50',
    isTerminal: false,
  },
  {
    etapa: 'GANADA',
    label: 'Ganada',
    colorDot: 'bg-emerald-400',
    colorHeader: 'text-emerald-700',
    colorBadge: 'bg-emerald-100 text-emerald-700 ring-emerald-200',
    colorBorder: 'border-emerald-200',
    colorBg: 'bg-emerald-50/50',
    isTerminal: true,
  },
  {
    etapa: 'PERDIDA',
    label: 'Perdida',
    colorDot: 'bg-red-400',
    colorHeader: 'text-red-700',
    colorBadge: 'bg-red-100 text-red-700 ring-red-200',
    colorBorder: 'border-red-200',
    colorBg: 'bg-red-50/50',
    isTerminal: true,
  },
];

@Component({
  selector: 'app-oportunidad-kanban',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './oportunidad-kanban.component.html',
})
export class OportunidadKanbanComponent implements OnInit {

  // ─── Dependencias ─────────────────────────────────────────────────────────
  private readonly oportunidadService = inject(OportunidadService);
  private readonly clienteService = inject(ClienteCrmService);
  private readonly fb = inject(FormBuilder);

  // ─── Constantes de plantilla ──────────────────────────────────────────────
  readonly columns: KanbanColumn[] = KANBAN_COLUMNS;

  // ─── Estado reactivo principal ────────────────────────────────────────────
  readonly oportunidades = signal<Oportunidad[]>([]);
  readonly clientes = signal<ClienteCRM[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string | null>(null);

  // ─── Computed: oportunidades agrupadas por etapa ──────────────────────────

  /** Mapa reactivo de etapa → lista de oportunidades de esa etapa */
  readonly oportunidadesPorEtapa = computed<Map<EtapaOportunidad, Oportunidad[]>>(() => {
    const map = new Map<EtapaOportunidad, Oportunidad[]>();
    for (const col of KANBAN_COLUMNS) {
      map.set(col.etapa, []);
    }
    for (const op of this.oportunidades()) {
      const lista = map.get(op.etapa);
      if (lista) lista.push(op);
    }
    return map;
  });

  /** Mapa reactivo de etapa → suma de montoEstimado */
  readonly montosPorEtapa = computed<Map<EtapaOportunidad, number>>(() => {
    const map = new Map<EtapaOportunidad, number>();
    for (const col of KANBAN_COLUMNS) {
      map.set(col.etapa, 0);
    }
    for (const op of this.oportunidades()) {
      const actual = map.get(op.etapa) ?? 0;
      map.set(op.etapa, actual + op.montoEstimado);
    }
    return map;
  });

  /** Métrica: total de oportunidades activas (no terminales) */
  readonly totalActivas = computed<number>(() =>
    this.oportunidades().filter(op =>
      op.etapa !== 'GANADA' && op.etapa !== 'PERDIDA'
    ).length
  );

  /** Métrica: suma total del pipeline activo */
  readonly montoTotalActivo = computed<number>(() =>
    this.oportunidades()
      .filter(op => op.etapa !== 'GANADA' && op.etapa !== 'PERDIDA')
      .reduce((sum, op) => sum + op.montoEstimado, 0)
  );

  /** Métrica: total de tratos ganados */
  readonly totalGanados = computed<number>(() =>
    this.oportunidades().filter(op => op.etapa === 'GANADA').length
  );

  // ─── Modal: Nueva Oportunidad ─────────────────────────────────────────────
  readonly isNuevoModalOpen = signal<boolean>(false);
  readonly isSubmittingNuevo = signal<boolean>(false);
  readonly nuevoError = signal<string | null>(null);

  readonly nuevoForm: FormGroup = this.fb.group({
    clienteId: [null, Validators.required],
    nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
    montoEstimado: [null, [Validators.required, Validators.min(0)]],
    probabilidad: [50, [Validators.required, Validators.min(0), Validators.max(100)]],
    fechaCierreEsperada: [null],
  });

  // ─── Modal: Motivo de Pérdida ─────────────────────────────────────────────
  readonly isPerdidaModalOpen = signal<boolean>(false);
  readonly isSubmittingPerdida = signal<boolean>(false);
  readonly perdidaError = signal<string | null>(null);
  readonly oportunidadParaPerdida = signal<Oportunidad | null>(null);

  readonly perdidaForm: FormGroup = this.fb.group({
    motivo: ['', [Validators.required, Validators.minLength(5)]],
  });

  // ─── Modal: Detalle de Oportunidad ───────────────────────────────────────
  readonly isDetalleModalOpen = signal<boolean>(false);
  readonly oportunidadDetalle = signal<Oportunidad | null>(null);

  // ─── Estado de carga por card ─────────────────────────────────────────────
  readonly loadingCardId = signal<string | null>(null);

  // ─── Ciclo de vida ────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.cargarDatos();
  }

  // ─── Carga de datos ───────────────────────────────────────────────────────

  cargarDatos(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.oportunidadService
      .getOportunidades()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (data) => this.oportunidades.set(data),
        error: (err) => {
          this.errorMessage.set(
            err?.error?.message ?? 'No se pudieron cargar las oportunidades.'
          );
        },
      });

    this.clienteService.getClientes().subscribe({
      next: (data) => this.clientes.set(data),
      error: () => this.clientes.set([]),
    });
  }

  /** Oportunidades de la columna especificada (desde el computed map) */
  getOpsPorEtapa(etapa: EtapaOportunidad): Oportunidad[] {
    return this.oportunidadesPorEtapa().get(etapa) ?? [];
  }

  /** Monto total de la columna especificada */
  getMontoEtapa(etapa: EtapaOportunidad): number {
    return this.montosPorEtapa().get(etapa) ?? 0;
  }

  // ─── Modal: Nueva Oportunidad ─────────────────────────────────────────────

  abrirNuevoModal(): void {
    this.nuevoForm.reset({
      clienteId: null,
      nombre: '',
      montoEstimado: null,
      probabilidad: 50,
      fechaCierreEsperada: null,
    });
    this.nuevoError.set(null);
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

    const raw = this.nuevoForm.value;
    const payload: OportunidadCreateRequest = {
      clienteId: raw.clienteId,
      nombre: raw.nombre,
      montoEstimado: parseFloat(raw.montoEstimado),
      probabilidad: parseInt(raw.probabilidad, 10),
      fechaCierreEsperada: raw.fechaCierreEsperada || undefined,
    };

    this.isSubmittingNuevo.set(true);
    this.nuevoError.set(null);

    this.oportunidadService
      .crear(payload)
      .pipe(finalize(() => this.isSubmittingNuevo.set(false)))
      .subscribe({
        next: (nueva) => {
          this.oportunidades.update((list) => [nueva, ...list]);
          this.cerrarNuevoModal();
        },
        error: (err) => {
          this.nuevoError.set(
            err?.error?.message ?? 'Error al crear la oportunidad.'
          );
        },
      });
  }

  // ─── Acciones de tarjeta ──────────────────────────────────────────────────

  /** Avanza la oportunidad a la etapa siguiente en el pipeline */
  avanzarEtapa(op: Oportunidad, nuevaEtapa: EtapaOportunidad): void {
    if (nuevaEtapa === 'PERDIDA') {
      // Solicitar motivo antes de proceder
      this.oportunidadParaPerdida.set(op);
      this.perdidaForm.reset({ motivo: '' });
      this.perdidaError.set(null);
      this.isPerdidaModalOpen.set(true);
      return;
    }

    if (nuevaEtapa === 'GANADA') {
      this.loadingCardId.set(op.id);
      this.oportunidadService
        .cerrarGanada(op.id)
        .pipe(finalize(() => this.loadingCardId.set(null)))
        .subscribe({
          next: (updated) => this.actualizarOportunidad(updated),
          error: (err) => alert(err?.error?.message ?? 'Error al cerrar como ganada.'),
        });
      return;
    }

    this.loadingCardId.set(op.id);
    this.oportunidadService
      .cambiarEtapa(op.id, { etapa: nuevaEtapa })
      .pipe(finalize(() => this.loadingCardId.set(null)))
      .subscribe({
        next: (updated) => this.actualizarOportunidad(updated),
        error: (err) => alert(err?.error?.message ?? 'Error al cambiar la etapa.'),
      });
  }

  /** Actualiza una oportunidad en el signal de forma reactiva */
  private actualizarOportunidad(updated: Oportunidad): void {
    this.oportunidades.update((list) =>
      list.map((op) => (op.id === updated.id ? updated : op))
    );
  }

  // ─── Modal: Motivo de Pérdida ─────────────────────────────────────────────

  cerrarPerdidaModal(): void {
    this.isPerdidaModalOpen.set(false);
    this.oportunidadParaPerdida.set(null);
  }

  onSubmitPerdida(): void {
    if (this.perdidaForm.invalid) {
      this.perdidaForm.markAllAsTouched();
      return;
    }

    const op = this.oportunidadParaPerdida();
    if (!op) return;

    const { motivo } = this.perdidaForm.value;
    this.isSubmittingPerdida.set(true);
    this.perdidaError.set(null);

    this.oportunidadService
      .cerrarPerdida(op.id, { motivo })
      .pipe(finalize(() => this.isSubmittingPerdida.set(false)))
      .subscribe({
        next: (updated) => {
          this.actualizarOportunidad(updated);
          this.cerrarPerdidaModal();
        },
        error: (err) => {
          this.perdidaError.set(
            err?.error?.message ?? 'Error al registrar la pérdida.'
          );
        },
      });
  }

  // ─── Modal: Detalle ───────────────────────────────────────────────────────

  abrirDetalle(op: Oportunidad): void {
    this.oportunidadDetalle.set(op);
    this.isDetalleModalOpen.set(true);
  }

  cerrarDetalle(): void {
    this.isDetalleModalOpen.set(false);
    this.oportunidadDetalle.set(null);
  }

  // ─── Helpers de plantilla ────────────────────────────────────────────────

  /** Obtiene la metadata de columna para una etapa dada */
  getColumnMeta(etapa: EtapaOportunidad): KanbanColumn {
    return KANBAN_COLUMNS.find((c) => c.etapa === etapa) ?? KANBAN_COLUMNS[0];
  }

  /** Formatea un número como moneda con separadores de miles */
  formatMonto(monto: number): string {
    return new Intl.NumberFormat('es-BO', {
      style: 'currency',
      currency: 'BOB',
      minimumFractionDigits: 0,
      maximumFractionDigits: 2,
    }).format(monto);
  }

  /** Retorna las etapas anteriores disponibles para retroceder */
  etapaAnterior(etapa: EtapaOportunidad): EtapaOportunidad | null {
    const order: EtapaOportunidad[] = ['CALIFICACION', 'PROPUESTA', 'NEGOCIACION'];
    const idx = order.indexOf(etapa);
    return idx > 0 ? order[idx - 1] : null;
  }

  /** Retorna la etapa siguiente disponible para avanzar */
  etapaSiguiente(etapa: EtapaOportunidad): EtapaOportunidad | null {
    const order: EtapaOportunidad[] = ['CALIFICACION', 'PROPUESTA', 'NEGOCIACION', 'GANADA'];
    const idx = order.indexOf(etapa);
    return idx >= 0 && idx < order.length - 1 ? order[idx + 1] : null;
  }

  /** Clases del badge de probabilidad */
  probabilidadBadgeClass(prob: number): string {
    if (prob >= 70) return 'bg-emerald-100 text-emerald-700 ring-emerald-200';
    if (prob >= 40) return 'bg-amber-100 text-amber-700 ring-amber-200';
    return 'bg-red-100 text-red-700 ring-red-200';
  }

  /** Verifica si un control del nuevoForm tiene error visible */
  hasNuevoError(control: string, error: string): boolean {
    const c = this.nuevoForm.get(control);
    return !!(c && c.touched && c.hasError(error));
  }

  /** true si el campo del nuevoForm es inválido y fue tocado */
  nuevoFieldInvalid(control: string): boolean {
    const c = this.nuevoForm.get(control);
    return !!(c && c.invalid && c.touched);
  }

  /** Indica si una tarjeta está siendo procesada */
  isCardLoading(id: string): boolean {
    return this.loadingCardId() === id;
  }

  /** Nombre de etapa legible */
  etapaLabel(etapa: EtapaOportunidad): string {
    const map: Record<EtapaOportunidad, string> = {
      CALIFICACION: 'Calificación',
      PROPUESTA: 'Propuesta',
      NEGOCIACION: 'Negociación',
      GANADA: 'Ganada',
      PERDIDA: 'Perdida',
    };
    return map[etapa] ?? etapa;
  }

  /** Convierte fecha ISO a formato legible */
  formatFecha(isoDate: string | null): string {
    if (!isoDate) return '—';
    const date = new Date(isoDate);
    return date.toLocaleDateString('es-BO', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  }
}
