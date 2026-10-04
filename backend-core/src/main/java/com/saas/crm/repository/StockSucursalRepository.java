package com.saas.crm.repository;

import com.saas.crm.domain.entity.StockSucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para {@link StockSucursal}.
 *
 * <p>Las consultas de stock siempre están acotadas por {@code tenantId}
 * o por la combinación {@code sucursalId + varianteProductoId}.</p>
 */
@Repository
public interface StockSucursalRepository extends JpaRepository<StockSucursal, Long> {

    /**
     * Busca el registro de stock de una variante en una sucursal concreta.
     * Usado para operaciones atómicas de reserva y consolidación.
     *
     * @param sucursalId         ID de la sucursal
     * @param varianteProductoId UUID de la variante
     * @return opcional con el registro de stock si existe
     */
    Optional<StockSucursal> findBySucursalIdAndVarianteProductoId(Integer sucursalId, UUID varianteProductoId);

    /**
     * Busca el registro de stock de una variante en una sucursal concreta,
     * validando además que pertenezca al tenant indicado.
     * Usado en la operación de ajuste de stock para garantizar aislamiento multi-tenant.
     *
     * @param sucursalId         ID de la sucursal
     * @param varianteProductoId UUID de la variante
     * @param tenantId           UUID del tenant
     * @return opcional con el registro de stock si existe
     */
    Optional<StockSucursal> findBySucursalIdAndVarianteProductoIdAndTenantId(
            Integer sucursalId, UUID varianteProductoId, UUID tenantId);

    /**
     * Lista todos los registros de stock de una sucursal dentro del tenant.
     *
     * @param sucursalId ID de la sucursal
     * @param tenantId   UUID del tenant
     * @return lista de stocks de la sucursal
     */
    List<StockSucursal> findBySucursalIdAndTenantId(Integer sucursalId, UUID tenantId);
}
