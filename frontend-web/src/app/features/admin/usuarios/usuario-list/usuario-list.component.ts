import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UsuarioEmpresaService } from '../../../../core/services/usuario-empresa.service';
import { AuthService } from '../../../../core/services/auth.service';
import { UsuarioColaborador } from '../../../../core/models/usuario.models';

@Component({
    selector: 'app-usuario-list',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule],
    templateUrl: './usuario-list.component.html',
})
export class UsuarioListComponent implements OnInit {
    private readonly usuarioEmpresaService = inject(UsuarioEmpresaService);
    public readonly authService = inject(AuthService);
    private readonly fb = inject(FormBuilder);

    usuarios = signal<UsuarioColaborador[]>([]);
    isLoading = signal<boolean>(false);
    isSubmitting = signal<boolean>(false);
    errorMsg = signal<string | null>(null);
    isModalOpen = signal<boolean>(false);

    usuarioForm: FormGroup = this.fb.group({
        nombreCompleto: ['', [Validators.required]],
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(6)]]
    });

    ngOnInit(): void {
        this.cargarUsuarios();
    }

    cargarUsuarios(): void {
        this.isLoading.set(true);
        this.usuarioEmpresaService.getUsuarios().subscribe({
            next: (data) => {
                this.usuarios.set(data);
                this.isLoading.set(false);
            },
            error: (err) => {
                this.errorMsg.set('Error al cargar la lista de colaboradores.');
                this.isLoading.set(false);
            }
        });
    }

    abrirModal(): void {
        this.errorMsg.set(null);
        this.usuarioForm.reset();
        this.isModalOpen.set(true);
    }

    cerrarModal(): void {
        this.isModalOpen.set(false);
    }

    onSubmit(): void {
        if (this.usuarioForm.invalid) {
            this.usuarioForm.markAllAsTouched();
            return;
        }

        this.isSubmitting.set(true);
        this.errorMsg.set(null);

        // backend ignores rolNombre in service, but we send it just in case backend expects it to pass validation of UsuarioCreateRequest
        const req = { ...this.usuarioForm.value, rolNombre: 'ROLE_VENDEDOR' };

        this.usuarioEmpresaService.crearVendedor(req).subscribe({
            next: (nuevo) => {
                this.usuarios.update(list => [nuevo, ...list]);
                this.isSubmitting.set(false);
                this.cerrarModal();
            },
            error: (err) => {
                this.isSubmitting.set(false);
                if (err.error && err.error.message) {
                    this.errorMsg.set(err.error.message);
                } else if (err.error && typeof err.error === 'string') {
                    this.errorMsg.set(err.error);
                } else {
                    this.errorMsg.set('Ocurrió un error al crear el vendedor. Valida el límite de tu plan SaaS.');
                }
            }
        });
    }

    toggleEstado(usuario: UsuarioColaborador): void {
        const confirmar = confirm(`¿Estás seguro de que deseas ${usuario.activo ? 'desactivar' : 'activar'} a ${usuario.nombreCompleto}?`);
        if (!confirmar) return;

        this.usuarioEmpresaService.toggleEstado(usuario.id).subscribe({
            next: (actualizado) => {
                this.usuarios.update(list => list.map(u => u.id === actualizado.id ? actualizado : u));
            },
            error: (err) => {
                alert(err.error?.message || 'Error al cambiar el estado del usuario.');
            }
        });
    }
}
