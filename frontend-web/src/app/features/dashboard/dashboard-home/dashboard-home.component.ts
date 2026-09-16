import { Component, inject, computed } from '@angular/core';
import { CommonModule, NgClass } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

/** Metadata de cada tarjeta KPI */
interface KpiCard {
  title: string;
  value: string;
  change: string;
  positive: boolean;
  icon: 'users' | 'shopping-bag' | 'currency' | 'shield';
  color: string;
}

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [CommonModule, NgClass],
  template: `
    <div class="px-4 py-6 sm:px-6 lg:px-8">

      <!-- ── Saludo de bienvenida ── -->
      <div class="mb-8">
        <h1 class="text-2xl font-bold tracking-tight text-slate-900">
          Bienvenido de vuelta
          @if (currentUser()?.email) {
            <span class="text-brand-primary">,
              {{ currentUser()!.email.split('@')[0] }}</span>
          }
          👋
        </h1>
        <p class="mt-1 text-sm text-slate-500">
          Aquí tienes un resumen actualizado de tu operación en tiempo real.
        </p>
      </div>

      <!-- ── GRID DE KPIs ── -->
      <section aria-labelledby="kpi-section-title">
        <h2 id="kpi-section-title" class="sr-only">Métricas clave del negocio</h2>
        <div class="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">

          @for (kpi of kpis; track kpi.title) {
            <article
              class="group relative overflow-hidden rounded-2xl bg-brand-surface
                     p-5 shadow-sm ring-1 ring-slate-200/70
                     transition-all duration-200
                     hover:-translate-y-0.5 hover:shadow-md hover:ring-slate-300">

              <!-- Gradiente decorativo -->
              <div class="pointer-events-none absolute -right-6 -top-6 h-24 w-24
                          rounded-full bg-gradient-to-br opacity-10 blur-2xl
                          transition-opacity duration-300 group-hover:opacity-20"
                   [ngClass]="kpi.color"
                   aria-hidden="true">
              </div>

              <!-- Icono -->
              <div class="mb-4 flex h-10 w-10 items-center justify-center
                          rounded-xl bg-gradient-to-br shadow-sm"
                   [ngClass]="kpi.color"
                   aria-hidden="true">
                @switch (kpi.icon) {
                  @case ('users') {
                    <svg class="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24"
                         stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round"
                            d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z"/>
                    </svg>
                  }
                  @case ('shopping-bag') {
                    <svg class="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24"
                         stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round"
                            d="M15.75 10.5V6a3.75 3.75 0 10-7.5 0v4.5m11.356-1.993l1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 01-1.12-1.243l1.264-12A1.125 1.125 0 015.513 7.5h12.974c.576 0 1.059.435 1.119 1.007z"/>
                    </svg>
                  }
                  @case ('currency') {
                    <svg class="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24"
                         stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round"
                            d="M12 6v12m-3-2.818l.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                    </svg>
                  }
                  @case ('shield') {
                    <svg class="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24"
                         stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round"
                            d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z"/>
                    </svg>
                  }
                }
              </div>

              <!-- Datos -->
              <p class="text-xs font-medium uppercase tracking-widest text-slate-400">
                {{ kpi.title }}
              </p>
              <p class="mt-1 text-2xl font-bold text-slate-900">{{ kpi.value }}</p>

              <!-- Badge de cambio -->
              <span class="mt-2 inline-flex items-center gap-0.5 rounded-full
                           px-2 py-0.5 text-xs font-semibold"
                    [ngClass]="kpi.positive
                      ? 'bg-emerald-50 text-emerald-700'
                      : 'bg-red-50 text-red-700'">
                @if (kpi.positive) {
                  <svg class="h-3 w-3" fill="none" viewBox="0 0 24 24"
                       stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                    <path stroke-linecap="round" stroke-linejoin="round"
                          d="M4.5 10.5L12 3m0 0l7.5 7.5M12 3v18"/>
                  </svg>
                }
                {{ kpi.change }}
              </span>

            </article>
          }
        </div>
      </section>

    </div>
  `,
})
export class DashboardHomeComponent {

  // ──────────────────────────────────────────
  // Dependencias
  // ──────────────────────────────────────────
  private readonly authService = inject(AuthService);

  /** Sesión del usuario activo (solo lectura) */
  protected readonly currentUser = this.authService.currentUser;

  /** true cuando el usuario es SuperAdmin */
  protected readonly isSuperAdmin = computed<boolean>(() =>
    this.currentUser()?.rol === 'ROLE_SUPER_ADMIN'
  );

  // ──────────────────────────────────────────
  // KPIs de ejemplo (MVP)
  // ──────────────────────────────────────────
  protected readonly kpis: KpiCard[] = [
    {
      title: 'Clientes Totales',
      value: '1,248',
      change: '+12%',
      positive: true,
      icon: 'users',
      color: 'from-blue-500 to-blue-600',
    },
    {
      title: 'Pedidos Hoy',
      value: '34',
      change: '+5',
      positive: true,
      icon: 'shopping-bag',
      color: 'from-violet-500 to-violet-600',
    },
    {
      title: 'Ingresos del Mes',
      value: '$28,490',
      change: '+8.3%',
      positive: true,
      icon: 'currency',
      color: 'from-emerald-500 to-emerald-600',
    },
    {
      title: 'Suscripción SaaS',
      value: 'Activa',
      change: 'Pro Plan',
      positive: true,
      icon: 'shield',
      color: 'from-amber-500 to-orange-500',
    },
  ];
}
