package com.saas.crm.service;

import com.saas.crm.domain.entity.Categoria;
import com.saas.crm.domain.entity.Tenant;
import com.saas.crm.dto.inventario.CategoriaCreateRequest;
import com.saas.crm.dto.inventario.CategoriaResponse;
import com.saas.crm.repository.CategoriaRepository;
import com.saas.crm.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de negocio para la gestión del árbol de categorías del catálogo.
 *
 * <p>Soporta categorías jerárquicas: al listar, devuelve únicamente las raíces
 * con sus subcategorías anidadas para representar el árbol completo.</p>
 */
@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final TenantRepository tenantRepository;

    // ─── Listar árbol de Categorías ───────────────────────────────────────────

    /**
     * Devuelve todas las categorías raíz del tenant con sus subcategorías anidadas.
     *
     * @param tenantId UUID del tenant autenticado
     * @return lista de {@link CategoriaResponse} representando el árbol
     */
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarArbol(UUID tenantId) {
        return categoriaRepository
                .findByTenantIdAndCategoriaPadreIsNullOrderByNombreAsc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Registrar Categoría ──────────────────────────────────────────────────

    /**
     * Registra una nueva categoría para el tenant autenticado.
     * Si se provee {@code categoriaPadreId}, la valida contra el mismo tenant.
     *
     * @param tenantId UUID del tenant
     * @param request  payload con nombre, descripción y padre opcional
     * @return {@link CategoriaResponse} con la categoría persistida
     */
    @Transactional
    public CategoriaResponse registrarCategoria(UUID tenantId, CategoriaCreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant no encontrado."));

        Categoria padre = null;
        if (request.categoriaPadreId() != null) {
            padre = categoriaRepository.findByIdAndTenantId(request.categoriaPadreId(), tenantId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Categoría padre no encontrada o no pertenece al tenant."));
        }

        Categoria categoria = new Categoria();
        categoria.setTenant(tenant);
        categoria.setCategoriaPadre(padre);
        categoria.setNombre(request.nombre());
        categoria.setDescripcion(request.descripcion());
        categoria.setActivo(true);

        Categoria saved = categoriaRepository.save(categoria);
        return toResponse(saved);
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    /** Convierte recursivamente una {@link Categoria} en su DTO de respuesta. */
    private CategoriaResponse toResponse(Categoria c) {
        List<CategoriaResponse> hijos = c.getSubcategorias()
                .stream()
                .map(this::toResponse)
                .toList();

        return new CategoriaResponse(
                c.getId(),
                c.getTenant().getId(),
                c.getCategoriaPadre() != null ? c.getCategoriaPadre().getId() : null,
                c.getNombre(),
                c.getDescripcion(),
                c.getActivo(),
                hijos,
                c.getCreatedAt()
        );
    }
}
