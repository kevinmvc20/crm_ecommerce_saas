import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

/** Metadata de cada enlace de navegación lateral */
interface NavItem {
  label: string;
  route: string;
  icon: 'dashboard' | 'crm' | 'crm-leads' | 'crm-oportunidades' | 'catalog' | 'orders' | 'settings';
}

// ─── Catálogos de ítems por rol ───────────────────────────────────────────────

const NAV_SUPER_ADMIN: NavItem[] = [
  { label: 'Dashboard',     route: '/admin/dashboard', icon: 'dashboard' },
  { label: 'Empresas SaaS', route: '/admin/tenants',   icon: 'crm'       },
  { label: 'Configuración', route: '/admin/config',    icon: 'settings'  },
];

const NAV_TENANT: NavItem[] = [
  { label: 'Dashboard',             route: '/admin/dashboard',          icon: 'dashboard'          },
  { label: 'CRM & Prospectos',      route: '/admin/crm/leads',          icon: 'crm-leads'          },
  { label: 'Pipeline de Ventas',    route: '/admin/crm/oportunidades',  icon: 'crm-oportunidades'  },
  { label: 'Catálogo & Stock',      route: '/admin/catalogo',           icon: 'catalog'            },
  { label: 'Ventas & Pedidos',      route: '/admin/ventas',             icon: 'orders'             },
  { label: 'Configuración',         route: '/admin/config',             icon: 'settings'           },
];

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './admin-dashboard.component.html',
})
export class AdminDashboardComponent {

  // ──────────────────────────────────────────────────────
  // Dependencias
  // ──────────────────────────────────────────────────────
  protected readonly authService = inject(AuthService);

  // ──────────────────────────────────────────────────────
  // Estado reactivo
  // ──────────────────────────────────────────────────────

  /** Controla la visibilidad del sidebar en viewports pequeños */
  protected readonly isSidebarOpen = signal<boolean>(false);

  /** Sesión del usuario actual (Signal de solo lectura) */
  protected readonly currentUser = this.authService.currentUser;

  /** TenantId del usuario activo (null para SuperAdmin) */
  protected readonly currentTenant = this.authService.currentTenant;

  // ──────────────────────────────────────────────────────
  // Computed signals derivados del rol
  // ──────────────────────────────────────────────────────

  /** true cuando el usuario es SuperAdmin */
  protected readonly isSuperAdmin = computed<boolean>(() =>
    this.currentUser()?.rol === 'ROLE_SUPER_ADMIN'
  );

  /**
   * Ítems del Sidebar según RBAC:
   * - ROLE_SUPER_ADMIN → vista global (incluye "Empresas SaaS")
   * - ROLE_ADMIN_EMPRESA / ROLE_VENDEDOR → vista de tenant (oculta "Empresas SaaS")
   */
  protected readonly menuItems = computed<NavItem[]>(() =>
    this.isSuperAdmin() ? NAV_SUPER_ADMIN : NAV_TENANT
  );

  /** Nombre del workspace según rol y tenant */
  protected readonly workspaceName = computed<string>(() => {
    const user = this.currentUser();
    if (!user) return 'Panel Administrativo';
    if (user.rol === 'ROLE_SUPER_ADMIN') return 'Panel Global SaaS';
    return user.nombreTenant ?? 'Panel Administrativo';
  });

  /** Etiqueta legible para el badge de rol */
  protected readonly roleLabel = computed<string>(() => {
    const rol = this.currentUser()?.rol ?? '';
    const labels: Record<string, string> = {
      ROLE_SUPER_ADMIN:   'Super Admin',
      ROLE_ADMIN_EMPRESA: 'Admin Empresa',
      ROLE_VENDEDOR:      'Vendedor',
    };
    return labels[rol] ?? rol;
  });

  // ──────────────────────────────────────────────────────
  // Acciones
  // ──────────────────────────────────────────────────────

  /** Alterna el sidebar en móvil */
  protected toggleSidebar(): void {
    this.isSidebarOpen.update(open => !open);
  }

  /** Cierra sesión y redirige a /login */
  protected onLogout(): void {
    this.authService.logout();
  }
}
