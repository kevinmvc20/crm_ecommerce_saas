package com.saas.crm.service;

import com.saas.crm.domain.entity.PlanSaaS;
import com.saas.crm.domain.entity.Tenant;
import com.saas.crm.dto.saas.TenantConsumoResponse;
import com.saas.crm.repository.ProductoRepository;
import com.saas.crm.repository.SucursalRepository;
import com.saas.crm.repository.TenantRepository;
import com.saas.crm.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Servicio de consulta de métricas de consumo del tenant autenticado.
 *
 * <p>Agrega contadores de usuarios, productos y sucursales, y los contrasta
 * con los límites del {@link PlanSaaS} vigente para exponer el estado de
 * cuotas al frontend.</p>
 *
 * <h3>Regla de límite -1 = ILIMITADO:</h3>
 * <ul>
 *   <li>Plan Business → {@code limiteProductos = -1} (ilimitado) y
 *       {@code sucursalesLimite = -1} (multi-sucursal).</li>
 *   <li>Plan Básico / Pro → {@code sucursalesLimite = 1}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TenantConsumoService {

    private final TenantRepository   tenantRepository;
    private final UsuarioRepository  usuarioRepository;
    private final ProductoRepository productoRepository;
    private final SucursalRepository sucursalRepository;

    // ─── Constante: nombre del plan con multi-sucursal ────────────────────────
    private static final String PLAN_BUSINESS = "Business";

    /**
     * Calcula y mapea todas las métricas de consumo del tenant indicado.
     *
     * @param tenantId UUID del tenant autenticado
     * @return {@link TenantConsumoResponse} con las métricas actuales y los límites del plan
     * @throws ResponseStatusException HTTP 404 si el tenant no existe
     */
    @Transactional(readOnly = true)
    public TenantConsumoResponse obtenerConsumoTenant(UUID tenantId) {

        // 1. Cargar tenant con su plan (LAZY → forzar inicialización dentro de tx)
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tenant no encontrado."));

        PlanSaaS plan = tenant.getPlanSaaS();

        // 2. Nombre y estado del plan
        String planNombre          = plan != null ? plan.getNombre()          : "Sin plan";
        String estadoSuscripcion   = tenant.getEstadoSuscripcion();
        LocalDateTime fechaVencim  = tenant.getFechaVencimientoPlan();

        // 3. Días restantes (0 si ya venció o fecha nula)
        long diasRestantes = 0L;
        if (fechaVencim != null) {
            long diff = ChronoUnit.DAYS.between(LocalDateTime.now(), fechaVencim);
            diasRestantes = Math.max(0L, diff);
        }

        // 4. Límites del plan (-1 = ilimitado)
        int usuariosLimite  = plan != null ? safeLimit(plan.getLimiteUsuarios())  : 0;
        int productosLimite = plan != null ? safeLimit(plan.getLimiteProductos()) : 0;

        // Sucursales: Business → -1 (ilimitado), otros planes → 1
        boolean esBusiness    = plan != null && PLAN_BUSINESS.equalsIgnoreCase(plan.getNombre());
        int sucursalesLimite  = esBusiness ? -1 : 1;

        // 5. Consumo actual
        long usuariosActuales   = usuarioRepository.countByTenantIdAndActivoTrue(tenantId);
        long productosActuales  = productoRepository.countByTenantIdAndActivoTrue(tenantId);
        long sucursalesActuales = sucursalRepository.countByTenantIdAndActivoTrue(tenantId);

        // 6. Flags de alerta (solo relevante cuando el límite es finito)
        boolean limiteUsuariosAlcanzado   = usuariosLimite  > 0 && usuariosActuales  >= usuariosLimite;
        boolean limiteProductosAlcanzado  = productosLimite > 0 && productosActuales >= productosLimite;

        return new TenantConsumoResponse(
                planNombre,
                estadoSuscripcion,
                diasRestantes,
                fechaVencim,
                usuariosActuales,
                usuariosLimite,
                productosActuales,
                productosLimite,
                sucursalesActuales,
                sucursalesLimite,
                limiteUsuariosAlcanzado,
                limiteProductosAlcanzado
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Normaliza el límite almacenado en BD: valores nulos se tratan como 0
     * (sin plan → sin cuota asignada). El valor -1 se devuelve tal cual
     * (indica ilimitado y es interpretado por el caller y el frontend).
     */
    private int safeLimit(Integer limite) {
        return limite != null ? limite : 0;
    }
}
