import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

/** Metadata de cada enlace de navegación lateral */
interface NavItem {
  label: string;
  route: string;
  icon: 'dashboard' | 'crm' | 'catalog' | 'orders' | 'settings';
}

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

  /** Nombre del workspace según rol y tenant */
  protected readonly workspaceName = computed<string>(() => {
    const user = this.currentUser();
    if (!user) return 'Panel Administrativo';
    if (user.rol === 'ROLE_SUPER_ADMIN') return 'Panel Global SaaS';
    return user.nombreTenant ?? 'Panel Administrativo';
  });

  /** Badge del rol legible */
  protected readonly roleLabel = computed<string>(() => {
    const rol = this.currentUser()?.rol ?? '';
    const labels: Record<string, string> = {
      ROLE_SUPER_ADMIN: 'Super Admin',
      ROLE_ADMIN: 'Admin',
      ROLE_USER: 'Usuario',
    };
    return labels[rol] ?? rol;
  });

  /** true cuando el usuario es SuperAdmin */
  protected readonly isSuperAdmin = computed<boolean>(() =>
    this.currentUser()?.rol === 'ROLE_SUPER_ADMIN'
  );

  // ──────────────────────────────────────────────────────
  // Navegación lateral
  // ──────────────────────────────────────────────────────
  protected readonly navItems: NavItem[] = [
    { label: 'Dashboard',        route: '/admin/dashboard', icon: 'dashboard' },
    { label: 'Empresas SaaS',    route: '/admin/tenants',   icon: 'crm'       },
    { label: 'Catálogo & Stock', route: '/admin/catalog',   icon: 'catalog'   },
    { label: 'Ventas & Pedidos', route: '/admin/orders',    icon: 'orders'    },
    { label: 'Configuración',    route: '/admin/settings',  icon: 'settings'  },
  ];


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
