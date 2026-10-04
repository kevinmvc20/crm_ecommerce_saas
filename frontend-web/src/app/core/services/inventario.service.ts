import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Sucursal,
  SucursalCreateRequest,
  Categoria,
  CategoriaCreateRequest,
  Producto,
  ProductoCreateRequest,
  ProductoUpdateRequest,
  VarianteProducto,
  VarianteAdicionalRequest,
  StockSucursal,
  AjusteStockRequest,
} from '../models/inventario.models';

const BASE = 'http://localhost:8080/api/v1/admin';

/**
 * Servicio HTTP para el Módulo 4: Catálogo e Inventario Multi-Sucursal.
 *
 * El header `Authorization: Bearer <token>` es inyectado automáticamente
 * por el interceptor funcional `jwtInterceptor` configurado en `app.config.ts`.
 */
@Injectable({ providedIn: 'root' })
export class InventarioService {
  private readonly http = inject(HttpClient);

  // ─── Sucursales ─────────────────────────────────────────────────────────────

  /**
   * Lista todas las sucursales del tenant autenticado ordenadas por nombre.
   * GET /api/v1/admin/sucursales
   */
  getSucursales(): Observable<Sucursal[]> {
    return this.http.get<Sucursal[]>(`${BASE}/sucursales`);
  }

  /**
   * Registra una nueva sucursal para el tenant autenticado.
   * POST /api/v1/admin/sucursales
   */
  crearSucursal(data: SucursalCreateRequest): Observable<Sucursal> {
    return this.http.post<Sucursal>(`${BASE}/sucursales`, data);
  }

  // ─── Categorías ─────────────────────────────────────────────────────────────

  /**
   * Devuelve el árbol de categorías raíz con sus subcategorías anidadas.
   * GET /api/v1/admin/categorias
   */
  getCategorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${BASE}/categorias`);
  }

  /**
   * Registra una nueva categoría (raíz o hija) para el tenant autenticado.
   * POST /api/v1/admin/categorias
   */
  crearCategoria(data: CategoriaCreateRequest): Observable<Categoria> {
    return this.http.post<Categoria>(`${BASE}/categorias`, data);
  }

  // ─── Productos ──────────────────────────────────────────────────────────────

  /**
   * Lista todos los productos del tenant del más reciente al más antiguo.
   * GET /api/v1/admin/productos
   */
  getProductos(): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${BASE}/productos`);
  }

  /**
   * Crea un producto con sus variantes e inicializa el stock si se especifica.
   * Aplica el candado SaaS en el backend (HTTP 400 si se supera el límite).
   * POST /api/v1/admin/productos
   */
  crearProducto(data: ProductoCreateRequest): Observable<Producto> {
    return this.http.post<Producto>(`${BASE}/productos`, data);
  }

  /**
   * Actualiza los datos descriptivos de un producto existente.
   * Solo modifica nombre, descripción y categoría; no afecta variantes ni stock.
   * PUT /api/v1/admin/productos/{id}
   */
  actualizarProducto(id: string, data: ProductoUpdateRequest): Observable<Producto> {
    return this.http.put<Producto>(`${BASE}/productos/${id}`, data);
  }

  /**
   * Alterna el estado activo/inactivo de un producto (baja lógica).
   * PATCH /api/v1/admin/productos/{id}/toggle-activo
   */
  toggleActivoProducto(id: string): Observable<Producto> {
    return this.http.patch<Producto>(`${BASE}/productos/${id}/toggle-activo`, {});
  }

  /**
   * Agrega una variante adicional a un producto existente sin duplicar el base.
   * POST /api/v1/admin/productos/{id}/variantes
   */
  agregarVariante(productoId: string, data: VarianteAdicionalRequest): Observable<VarianteProducto> {
    return this.http.post<VarianteProducto>(`${BASE}/productos/${productoId}/variantes`, data);
  }

  /**
   * Ajusta el stock físico y mínimo de una variante en una sucursal específica.
   * POST /api/v1/admin/productos/variantes/{varianteId}/stock/sucursales/{sucursalId}
   */
  ajustarStock(varianteId: string, sucursalId: number, data: AjusteStockRequest): Observable<StockSucursal> {
    return this.http.post<StockSucursal>(
      `${BASE}/productos/variantes/${varianteId}/stock/sucursales/${sucursalId}`,
      data
    );
  }
}
