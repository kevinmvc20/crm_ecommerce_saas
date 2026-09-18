import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
    {
        path: '',
        redirectTo: 'login',
        pathMatch: 'full'
    },
    {
        path: 'login',
        loadComponent: () =>
            import('./features/auth/login/login.component').then(m => m.LoginComponent),
    },
    {
        // Layout administrativo: el AdminDashboardComponent actúa como shell
        // con Sidebar + Header permanentes. Las rutas hijas se renderizan
        // dentro de su <router-outlet> sin desmontar el layout.
        path: 'admin',
        loadComponent: () =>
            import('./features/dashboard/admin-dashboard/admin-dashboard.component').then(
                m => m.AdminDashboardComponent
            ),
        canActivate: [authGuard],
        children: [
            {
                // /admin/dashboard — vista de KPIs y bienvenida
                path: 'dashboard',
                loadComponent: () =>
                    import('./features/dashboard/dashboard-home/dashboard-home.component').then(
                        m => m.DashboardHomeComponent
                    ),
            },
            {
                // /admin/tenants — módulo de gestión de empresas SaaS
                path: 'tenants',
                loadComponent: () =>
                    import('./features/tenants/tenant-list/tenant-list.component').then(
                        m => m.TenantListComponent
                    ),
            },
            {
                // /admin/crm/leads — módulo CRM de gestión de prospectos
                path: 'crm/leads',
                loadComponent: () =>
                    import('./features/crm/leads/lead-list/lead-list.component').then(
                        m => m.LeadListComponent
                    ),
            },
            {
                // /admin/crm/oportunidades — Tablero Kanban de Oportunidades Comerciales
                path: 'crm/oportunidades',
                loadComponent: () =>
                    import('./features/crm/oportunidades/oportunidad-kanban/oportunidad-kanban.component').then(
                        m => m.OportunidadKanbanComponent
                    ),
            },
            {
                // /admin/usuarios — Gestión de Equipo Comercial / Vendedores
                path: 'usuarios',
                loadComponent: () =>
                    import('./features/admin/usuarios/usuario-list/usuario-list.component').then(
                        m => m.UsuarioListComponent
                    ),
            },
            {
                path: '',
                redirectTo: 'dashboard',
                pathMatch: 'full',
            },
        ],
    },
    {
        path: '**',
        redirectTo: 'login',
    },
];