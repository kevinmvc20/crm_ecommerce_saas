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

  // ── Modales ──────────────────────────────────────────────────────────────────
  readonly showModalProducto  = signal(false);
  readonly showModalCategoria = signal(false);
  readonly showModalSucursal  = signal(false);
  readonly isSaving           = signal(false);
  readonly saveError          = signal<string | null>(null);

  // ── Categorías aplanadas (para selector en modal producto) ──────────────────
  readonly categoriasFlat = computed<CategoriaFlat[]>(() =>
    this.flattenCategorias(this.categorias(), 0)
  );

  // ── Computed: stock total por producto ──────────────────────────────────────
  readonly productosConStock = computed(() =>
    this.productos().map(p => ({
      ...p,
      totalVariantes: p.variantes.length,
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

  // ── Modal Producto ──────────────────────────────────────────────────────────

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
