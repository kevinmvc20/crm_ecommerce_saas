export interface UsuarioColaborador {
    id: string;
    email: string;
    nombreCompleto: string;
    rolNombre: string;
    activo: boolean;
    createdAt: string;
}

export interface UsuarioCreateRequest {
    email: string;
    password?: string; // opcional si se envía por partes o requerido
    nombreCompleto: string;
    rolNombre?: string;
}
