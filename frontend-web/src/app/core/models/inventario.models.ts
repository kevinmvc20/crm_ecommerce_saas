// ─── Sucursal ─────────────────────────────────────────────────────────────────

/**
 * Representación de una Sucursal (punto de venta / bodega) devuelta por el backend.
 * Mapea el record SucursalResponse de Java.
 */
export interface Sucursal {
  id: number;
  tenantId: string;
  nombre: string;
  direccion: string | null;
  telefono: string | null;
  activo: boolean;
  createdAt: string; // ISO-8601
  updatedAt: string; // ISO-8601
}

/**
 * Payload para POST /api/v1/admin/sucursales.
 */
export interface SucursalCreateRequest {
  nombre: string;
  direccion?: string;
  telefono?: string;
}

export interface SucursalUpdateRequest {
  nombre: string;
  direccion?: string;
  telefono?: string;
}

// ─── Categoria ────────────────────────────────────────────────────────────────

/**
 * Nodo de la jerarquía de categorías del catálogo.
 * Las subcategorías vienen anidadas en la propiedad `subcategorias`.
 * Mapea el record CategoriaResponse de Java.
 */
export interface Categoria {
  id: number;
  tenantId: string;
  categoriaPadreId: number | null;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
  subcategorias: Categoria[];
  createdAt: string; // ISO-8601
}

/**
 * Payload para POST /api/v1/admin/categorias.
 */
export interface CategoriaCreateRequest {
  nombre: string;
  descripcion?: string;
  /** null o ausente para categorías raíz. */
  categoriaPadreId?: number | null;
}

export interface CategoriaUpdateRequest {
  nombre: string;
  descripcion?: string;
  categoriaPadreId?: number | null;
}

// ─── Variante de Producto ─────────────────────────────────────────────────────

/**
 * Variante de compra de un producto (talla, color, capacidad, etc.).
 * Mapea el record VarianteResponse de Java.
 * `stocks` es populado opcionalmente al expandir la fila del acordeón.
 */
export interface VarianteProducto {
  id: string;
  productoId: string;
  sku: string;
  nombreVariante: string;
  precio: number;
  activo: boolean;
  createdAt: string; // ISO-8601
  updatedAt: string; // ISO-8601
  /** Desglose de existencias por sucursal (cargado bajo demanda). */
  stocks?: StockSucursal[];
}

/**
 * Payload para una variante dentro de ProductoCreateRequest.
 */
export interface VarianteCreateRequest {
  sku: string;
  nombreVariante: string;
  precio: number;
  /** Unidades físicas iniciales (opcional). */
  stockInicial?: number;
  /** Sucursal donde registrar el stock inicial (requerido si stockInicial > 0). */
  sucursalIdInicial?: number;
}

/**
 * Payload para POST /api/v1/admin/productos/{id}/variantes.
 * Agrega una variante adicional a un producto existente.
 */
export interface VarianteAdicionalRequest {
  sku: string;
  nombreVariante: string;
  precio: number;
  stockInicial?: number;
  sucursalId?: number;
}

// ─── Producto ─────────────────────────────────────────────────────────────────

/**
 * Producto del catálogo con sus variantes.
 * Mapea el record ProductoResponse de Java.
 */
export interface Producto {
  id: string;
  tenantId: string;
  categoriaId: number | null;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
  variantes: VarianteProducto[];
  createdAt: string; // ISO-8601
  updatedAt: string; // ISO-8601
}

/**
 * Payload para POST /api/v1/admin/productos.
 */
export interface ProductoCreateRequest {
  nombre: string;
  descripcion?: string;
  categoriaId?: number | null;
  variantes: VarianteCreateRequest[];
}

/**
 * Payload para PUT /api/v1/admin/productos/{id}.
 * Actualiza únicamente los datos descriptivos del producto.
 */
export interface ProductoUpdateRequest {
  nombre: string;
  descripcion?: string;
  categoriaId?: number | null;
}

// ─── Stock por Sucursal ───────────────────────────────────────────────────────

/**
 * Estado de stock de una variante en una sucursal concreta.
 * Mapea el record StockResponse de Java.
 */
export interface StockSucursal {
  id: number;
  sucursalId: number;
  varianteProductoId: string;
  stockFisico: number;
  stockReservado: number;
  stockDisponible: number;
  stockMinimo: number;
  updatedAt: string; // ISO-8601
}

/**
 * Payload para ajustar el stock de una variante en una sucursal.
 */
export interface AjusteStockRequest {
  nuevoStockFisico: number;
  stockMinimo: number;
}
