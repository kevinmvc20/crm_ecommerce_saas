import {
  Component,
  inject,
  signal,
  computed,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormArray, Validators, AbstractControl } from '@angular/forms';
import { InventarioService } from '../../../core/services/inventario.service';
import {
  Sucursal,
  Categoria,
  Producto,
  VarianteProducto,
  StockSucursal,
} from '../../../core/models/inventario.models';

/** Tipo de pestaña activa */
type ActiveTab = 'productos' | 'categorias' | 'sucursales';

/** Categoría aplanada con nivel para render en selector */
interface CategoriaFlat {
  id: number;
  label: string;
  level: number;
}

@Component({
  selector: 'app-catalogo-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './catalogo-dashboard.component.html',
})
export class CatalogoDashboardComponent implements OnInit {

  // ── Dependencias ────────────────────────────────────────────────────────────
  private readonly svc = inject(InventarioService);
  private readonly fb  = inject(FormBuilder);

  // ── Estado general ──────────────────────────────────────────────────────────
  readonly activeTab    = signal<ActiveTab>('productos');
  readonly isLoading    = signal(false);
  readonly errorMsg     = signal<string | null>(null);

  // ── Datos ───────────────────────────────────────────────────────────────────
  readonly productos    = signal<Producto[]>([]);
  readonly categorias   = signal<Categoria[]>([]);
  readonly sucursales   = signal<Sucursal[]>([]);

  // ── Modales existentes ───────────────────────────────────────────────────────
  readonly showModalProducto  = signal(false);
  readonly showModalCategoria = signal(false);
  readonly showModalSucursal  = signal(false);
  readonly isSaving           = signal(false);
  readonly saveError          = signal<string | null>(null);

  // ── Nuevos Signals para Módulo 4 ─────────────────────────────────────────────

  /** IDs de productos con la sub-fila acordeón abierta. */
  readonly expandedProductIds = signal<Set<string>>(new Set());

  /** Controla visibilidad del modal de añadir variante. */
  readonly showVarianteModal = signal<boolean>(false);

  /** Controla visibilidad del modal de ajuste de stock. */
  readonly showAjusteStockModal = signal<boolean>(false);

  /** Controla visibilidad del modal de editar producto. */
  readonly showEditarProductoModal = signal<boolean>(false);

  /** Producto seleccionado actualmente para operar sobre él. */
  readonly productoSeleccionado = signal<Producto | null>(null);

  /** Variante seleccionada para el modal de ajuste de stock. */
  readonly varianteSeleccionada = signal<VarianteProducto | null>(null);

  // ── Formulario: Añadir Variante Adicional ───────────────────────────────────
  readonly varianteAdicionalForm = this.fb.group({
    sku:             ['', [Validators.required, Validators.maxLength(60)]],
    nombreVariante:  ['', [Validators.required, Validators.maxLength(100)]],
    precio:          [0, [Validators.required, Validators.min(0)]],
    stockInicial:    [null as number | null],
    sucursalId:      [null as number | null],
  });

  // ── Formulario: Ajustar Stock ───────────────────────────────────────────────
  readonly ajusteStockForm = this.fb.group({
    sucursalId:       [null as number | null, Validators.required],
    nuevoStockFisico: [0, [Validators.required, Validators.min(0)]],
    stockMinimo:      [0, [Validators.required, Validators.min(0)]],
  });

  // ── Formulario: Editar Producto ─────────────────────────────────────────────
  readonly editarProductoForm = this.fb.group({
    nombre:      ['', [Validators.required, Validators.maxLength(150)]],
    descripcion: [''],
    categoriaId: [null as number | null],
  });

  // ── Categorías aplanadas (para selector en modal producto) ──────────────────
  readonly categoriasFlat = computed<CategoriaFlat[]>(() =>
    this.flattenCategorias(this.categorias(), 0)
  );

  /** Solo categorías de primer nivel (raíces) para el selector de categoría padre */
  readonly categoriasRaiz = computed<CategoriaFlat[]>(() =>
    this.categoriasFlat().filter(c => c.level === 0)
  );

  // ── Computed: stock total por producto ──────────────────────────────────────
  readonly productosConStock = computed(() =>
    this.productos().map(p => ({
      ...p,
      totalVariantes: p.variantes.length,
      totalStock: this.calcTotalStock(p),
      almacenesConStock: this.getAlmacenesConStock(p),
    }))
  );

  // ── Formularios ReactiveFormsModule ─────────────────────────────────────────

  readonly productoForm = this.fb.group({
    nombre:      ['', [Validators.required, Validators.maxLength(150)]],
    descripcion: [''],
    categoriaId: [null as number | null],
    variantes: this.fb.array([this.buildVarianteGroup()]),
  });

  readonly categoriaForm = this.fb.group({
    nombre:           ['', [Validators.required, Validators.maxLength(100)]],
    descripcion:      [''],
    categoriaPadreId: [null as number | null],
  });

  readonly sucursalForm = this.fb.group({
    nombre:    ['', [Validators.required, Validators.maxLength(120)]],
    direccion: ['', Validators.maxLength(250)],
    telefono:  ['', Validators.maxLength(30)],
  });

  // ── Helpers de acceso a FormArray ───────────────────────────────────────────
  get variantesArray(): FormArray {
    return this.productoForm.get('variantes') as FormArray;
  }

  // ── Lifecycle ───────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadAll();
  }

  // ── Carga de datos ──────────────────────────────────────────────────────────

  private loadAll(): void {
    this.isLoading.set(true);
    this.errorMsg.set(null);

    this.svc.getProductos().subscribe({
      next: data => this.productos.set(data),
      error: () => this.errorMsg.set('No se pudieron cargar los productos.'),
    });

    this.svc.getCategorias().subscribe({
      next: data => this.categorias.set(data),
    });

    this.svc.getSucursales().subscribe({
      next: data => {
        this.sucursales.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  // ── Navegación de pestañas ──────────────────────────────────────────────────

  setTab(tab: ActiveTab): void {
    this.activeTab.set(tab);
  }

  // ── Acordeón (Expand / Collapse) ────────────────────────────────────────────

  /**
   * Abre o cierra la sub-fila de detalle de variantes para el producto indicado.
   * Usa inmutabilidad creando un nuevo Set en cada toggleado.
   */
  toggleExpand(prodId: string): void {
    this.expandedProductIds.update(set => {
      const next = new Set(set);
      if (next.has(prodId)) {
        next.delete(prodId);
      } else {
        next.add(prodId);
      }
      return next;
    });
  }

  isExpanded(prodId: string): boolean {
    return this.expandedProductIds().has(prodId);
  }

  // ── Modal: Añadir Variante Adicional ───────────────────────────────────────

  abrirModalVariante(prod: Producto): void {
    this.productoSeleccionado.set(prod);
    this.varianteAdicionalForm.reset({ sku: '', nombreVariante: '', precio: 0, stockInicial: null, sucursalId: null });
    this.saveError.set(null);
    this.showVarianteModal.set(true);
  }

  cerrarModalVariante(): void {
    this.showVarianteModal.set(false);
    this.productoSeleccionado.set(null);
  }

  submitVariante(): void {
    if (this.varianteAdicionalForm.invalid) {
      this.varianteAdicionalForm.markAllAsTouched();
      return;
    }
    const prod = this.productoSeleccionado();
    if (!prod) return;

    this.isSaving.set(true);
    this.saveError.set(null);

    const raw = this.varianteAdicionalForm.getRawValue();
    this.svc.agregarVariante(prod.id, {
      sku:            raw.sku!,
      nombreVariante: raw.nombreVariante!,
      precio:         Number(raw.precio),
      stockInicial:   raw.stockInicial ? Number(raw.stockInicial) : undefined,
      sucursalId:     raw.sucursalId ? Number(raw.sucursalId) : undefined,
    }).subscribe({
      next: nuevaVariante => {
        // Actualizar la lista de productos en el signal sin recargar todo
        this.productos.update(list =>
          list.map(p =>
            p.id === prod.id
              ? { ...p, variantes: [...p.variantes, nuevaVariante] }
              : p
          )
        );
        this.isSaving.set(false);
        this.showVarianteModal.set(false);
      },
      error: err => {
        this.saveError.set(err?.error?.message ?? 'Error al agregar la variante.');
        this.isSaving.set(false);
      },
    });
  }

  // ── Modal: Ajustar Stock ────────────────────────────────────────────────────

  abrirModalAjusteStock(prod: Producto, variante: VarianteProducto): void {
    this.productoSeleccionado.set(prod);
    this.varianteSeleccionada.set(variante);
    this.ajusteStockForm.reset({ sucursalId: null, nuevoStockFisico: 0, stockMinimo: 5 });
    this.saveError.set(null);
    this.showAjusteStockModal.set(true);
  }

  cerrarModalAjusteStock(): void {
    this.showAjusteStockModal.set(false);
    this.varianteSeleccionada.set(null);
    this.productoSeleccionado.set(null);
  }

  submitAjusteStock(): void {
    if (this.ajusteStockForm.invalid) {
      this.ajusteStockForm.markAllAsTouched();
      return;
    }
    const variante = this.varianteSeleccionada();
    if (!variante) return;

    this.isSaving.set(true);
    this.saveError.set(null);

    const raw = this.ajusteStockForm.getRawValue();
    this.svc.ajustarStock(variante.id, Number(raw.sucursalId), {
      nuevoStockFisico: Number(raw.nuevoStockFisico),
      stockMinimo:      Number(raw.stockMinimo),
    }).subscribe({
      next: _stockActualizado => {
        this.isSaving.set(false);
        this.showAjusteStockModal.set(false);
        // Recargar lista completa para reflejar stock actualizado
        this.svc.getProductos().subscribe({ next: data => this.productos.set(data) });
      },
      error: err => {
        this.saveError.set(err?.error?.message ?? 'Error al ajustar el stock.');
        this.isSaving.set(false);
      },
    });
  }

  // ── Modal: Editar Producto ──────────────────────────────────────────────────

  abrirModalEditar(prod: Producto): void {
    this.productoSeleccionado.set(prod);
    this.editarProductoForm.reset({
      nombre:      prod.nombre,
      descripcion: prod.descripcion ?? '',
      categoriaId: prod.categoriaId,
    });
    this.saveError.set(null);
    this.showEditarProductoModal.set(true);
  }

  cerrarModalEditar(): void {
    this.showEditarProductoModal.set(false);
    this.productoSeleccionado.set(null);
  }

  submitEditar(): void {
    if (this.editarProductoForm.invalid) {
      this.editarProductoForm.markAllAsTouched();
      return;
    }
    const prod = this.productoSeleccionado();
    if (!prod) return;

    this.isSaving.set(true);
    this.saveError.set(null);

    const raw = this.editarProductoForm.getRawValue();
    this.svc.actualizarProducto(prod.id, {
      nombre:      raw.nombre!,
      descripcion: raw.descripcion || undefined,
      categoriaId: raw.categoriaId ?? undefined,
    }).subscribe({
      next: updated => {
        this.productos.update(list =>
          list.map(p => p.id === updated.id ? { ...updated, variantes: p.variantes } : p)
        );
        this.isSaving.set(false);
        this.showEditarProductoModal.set(false);
      },
      error: err => {
        this.saveError.set(err?.error?.message ?? 'Error al actualizar el producto.');
        this.isSaving.set(false);
      },
    });
  }

  // ── Toggle Estado Producto ──────────────────────────────────────────────────

  toggleEstadoProducto(prod: Producto): void {
    this.svc.toggleActivoProducto(prod.id).subscribe({
      next: updated => {
        this.productos.update(list =>
          list.map(p => p.id === updated.id ? { ...updated, variantes: p.variantes } : p)
        );
      },
      error: err => {
        this.errorMsg.set(err?.error?.message ?? 'Error al cambiar el estado del producto.');
      },
    });
  }

  // ── Modal Producto (crear nuevo) ────────────────────────────────────────────

  openModalProducto(): void {
    this.productoForm.reset({ nombre: '', descripcion: '', categoriaId: null });
    while (this.variantesArray.length > 0) this.variantesArray.removeAt(0);
    this.variantesArray.push(this.buildVarianteGroup());
    this.saveError.set(null);
    this.showModalProducto.set(true);
  }

  closeModalProducto(): void { this.showModalProducto.set(false); }

  addVariante(): void {
    this.variantesArray.push(this.buildVarianteGroup());
  }

  removeVariante(index: number): void {
    if (this.variantesArray.length > 1) this.variantesArray.removeAt(index);
  }

  submitProducto(): void {
    if (this.productoForm.invalid) {
      this.productoForm.markAllAsTouched();
      return;
    }
    this.isSaving.set(true);
    this.saveError.set(null);

    const raw = this.productoForm.getRawValue();
    const payload = {
      nombre:      raw.nombre!,
      descripcion: raw.descripcion || undefined,
      categoriaId: raw.categoriaId ?? undefined,
      variantes:   raw.variantes.map((v: any) => ({
        sku:             v.sku,
        nombreVariante:  v.nombreVariante,
        precio:          Number(v.precio),
        stockInicial:    v.stockInicial ? Number(v.stockInicial) : undefined,
        sucursalIdInicial: v.sucursalIdInicial ? Number(v.sucursalIdInicial) : undefined,
      })),
    };

    this.svc.crearProducto(payload).subscribe({
      next: prod => {
        this.productos.update(list => [prod, ...list]);
        this.isSaving.set(false);
        this.showModalProducto.set(false);
      },
      error: (err) => {
        const msg = err?.error?.message ?? err?.message ?? 'Error al crear el producto.';
        this.saveError.set(msg);
        this.isSaving.set(false);
      },
    });
  }

  // ── Modal Categoría ─────────────────────────────────────────────────────────

  openModalCategoria(): void {
    this.categoriaForm.reset({ nombre: '', descripcion: '', categoriaPadreId: null });
    this.saveError.set(null);
    this.showModalCategoria.set(true);
  }

  closeModalCategoria(): void { this.showModalCategoria.set(false); }

  submitCategoria(): void {
    if (this.categoriaForm.invalid) {
      this.categoriaForm.markAllAsTouched();
      return;
    }
    this.isSaving.set(true);
    this.saveError.set(null);

    const raw = this.categoriaForm.getRawValue();
    this.svc.crearCategoria({
      nombre:           raw.nombre!,
      descripcion:      raw.descripcion || undefined,
      categoriaPadreId: raw.categoriaPadreId ?? undefined,
    }).subscribe({
      next: cat => {
        this.categorias.update(list => {
          if (!cat.categoriaPadreId) return [...list, cat];
          return list.map(c => this.insertSubcategoria(c, cat));
        });
        this.isSaving.set(false);
        this.showModalCategoria.set(false);
      },
      error: (err) => {
        this.saveError.set(err?.error?.message ?? 'Error al crear la categoría.');
        this.isSaving.set(false);
      },
    });
  }

  // ── Modal Sucursal ──────────────────────────────────────────────────────────

  openModalSucursal(): void {
    this.sucursalForm.reset({ nombre: '', direccion: '', telefono: '' });
    this.saveError.set(null);
    this.showModalSucursal.set(true);
  }

  closeModalSucursal(): void { this.showModalSucursal.set(false); }

  submitSucursal(): void {
    if (this.sucursalForm.invalid) {
      this.sucursalForm.markAllAsTouched();
      return;
    }
    this.isSaving.set(true);
    this.saveError.set(null);

    const raw = this.sucursalForm.getRawValue();
    this.svc.crearSucursal({
      nombre:    raw.nombre!,
      direccion: raw.direccion || undefined,
      telefono:  raw.telefono  || undefined,
    }).subscribe({
      next: suc => {
        this.sucursales.update(list => [...list, suc]);
        this.isSaving.set(false);
        this.showModalSucursal.set(false);
      },
      error: (err) => {
        this.saveError.set(err?.error?.message ?? 'Error al crear la sucursal.');
        this.isSaving.set(false);
      },
    });
  }

  // ── Helpers UI ──────────────────────────────────────────────────────────────

  formatPrecio(n: number): string {
    return new Intl.NumberFormat('es-BO', { style: 'currency', currency: 'BOB' }).format(n);
  }

  trackById(_: number, item: { id: string | number }): string | number {
    return item.id;
  }

  /** Controles de un grupo de variante */
  getVarianteControls(i: number): { [key: string]: AbstractControl } {
    return (this.variantesArray.at(i) as any).controls;
  }

  /**
   * Nombre de sucursal por ID para mostrar en el badge de stock.
   */
  getSucursalNombre(sucursalId: number): string {
    return this.sucursales().find(s => s.id === sucursalId)?.nombre ?? `Suc. ${sucursalId}`;
  }

  /**
   * Calcula el stock total disponible de un producto sumando el stockInicial
   * registrado al crear las variantes (dato disponible en el formulario).
   * En la vista de tabla, las variantes del backend no incluyen stock, por lo
   * que devolvemos 0 como valor de referencia visual.
   */
  calcTotalStock(p: Producto): number {
    // La API no retorna stock en el listado de variantes; retornamos 0 como placeholder.
    return 0;
  }

  /**
   * Devuelve los nombres de almacenes que tienen existencias para el producto.
   * Placeholder: la API de listado no incluye desglose por sucursal en este endpoint.
   */
  getAlmacenesConStock(p: Producto): string[] {
    return [];
  }

  /**
   * Verifica si alguna variante del formulario tiene stockInicial > 0
   * pero sin sucursalIdInicial asignada.
   */
  hasVariantesSinSucursal(): boolean {
    return this.variantesArray.controls.some(ctrl => {
      const stock = ctrl.get('stockInicial')?.value;
      const sucursal = ctrl.get('sucursalIdInicial')?.value;
      return (stock !== null && Number(stock) > 0) && !sucursal;
    });
  }

  /**
   * Determina si el campo sucursalIdInicial de una variante es inválido
   * (stockInicial > 0 pero sucursal vacía y el campo fue tocado o el form fue enviado).
   */
  isSucursalRequerida(index: number): boolean {
    const ctrl = this.variantesArray.at(index);
    const stock = ctrl.get('stockInicial')?.value;
    const sucursal = ctrl.get('sucursalIdInicial')?.value;
    return (stock !== null && Number(stock) > 0) && !sucursal;
  }

  /** Convierte árbol recursivo a lista plana con nivel de indentación */
  private flattenCategorias(cats: Categoria[], level: number): CategoriaFlat[] {
    const result: CategoriaFlat[] = [];
    for (const c of cats) {
      result.push({ id: c.id, label: c.nombre, level });
      if (c.subcategorias?.length) {
        result.push(...this.flattenCategorias(c.subcategorias, level + 1));
      }
    }
    return result;
  }

  private buildVarianteGroup() {
    return this.fb.group({
      sku:              ['', [Validators.required, Validators.maxLength(60)]],
      nombreVariante:   ['', [Validators.required, Validators.maxLength(100)]],
      precio:           [0, [Validators.required, Validators.min(0)]],
      stockInicial:     [null as number | null],
      sucursalIdInicial:[null as number | null],
    });
  }

  private insertSubcategoria(parent: Categoria, child: Categoria): Categoria {
    if (parent.id === child.categoriaPadreId) {
      return { ...parent, subcategorias: [...(parent.subcategorias ?? []), child] };
    }
    return {
      ...parent,
      subcategorias: (parent.subcategorias ?? []).map(s => this.insertSubcategoria(s, child)),
    };
  }
}
