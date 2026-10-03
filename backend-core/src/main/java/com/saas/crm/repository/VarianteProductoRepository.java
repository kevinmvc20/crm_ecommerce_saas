package com.saas.crm.repository;

import com.saas.crm.domain.entity.VarianteProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para {@link VarianteProducto}.
 *
 * <p>Permite verificar unicidad de SKU dentro del tenant y recuperar
 * variantes por producto.</p>
 */
@Repository
public interface VarianteProductoRepository extends JpaRepository<VarianteProducto, UUID> {

    /**
     * Busca una variante por SKU dentro del alcance del tenant.
     * Usado para detectar duplicados de SKU en la creación.
     *
     * @param sku      código de variante
     * @param tenantId UUID del tenant
     * @return opcional con la variante si el SKU ya existe en el tenant
     */
    Optional<VarianteProducto> findBySkuAndTenantId(String sku, UUID tenantId);

    /**
     * Lista todas las variantes de un producto concreto dentro del tenant.
     *
     * @param productoId UUID del producto padre
     * @param tenantId   UUID del tenant
     * @return lista de variantes del producto
     */
    List<VarianteProducto> findByProductoIdAndTenantId(UUID productoId, UUID tenantId);
}
