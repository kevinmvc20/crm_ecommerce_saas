/**
 * Respuesta del endpoint GET /api/v1/admin/tenant/consumo
 * Representa el uso actual de recursos del tenant autenticado.
 * Los límites con valor -1 indican "sin límite" (Ilimitado).
 */
export interface TenantConsumo {
  planNombre: string;
  estadoSuscripcion: string;
  diasRestantesMembresia: number;
  fechaVencimientoPlan: string;
  usuariosActuales: number;
  usuariosLimite: number;
  productosActuales: number;
  productosLimite: number;
  sucursalesActuales: number;
  sucursalesLimite: number;
  limiteUsuariosAlcanzado: boolean;
  limiteProductosAlcanzado: boolean;
}
