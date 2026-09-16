package com.saas.crm.controller.saas;

import com.saas.crm.dto.saas.PlanSaaSDTO;
import com.saas.crm.service.PlanSaaSService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para la consulta de planes SaaS disponibles.
 * Base URL: {@code /api/v1/planes}
 */
@RestController
@RequestMapping("/api/v1/planes")
@RequiredArgsConstructor
public class PlanSaaSController {

    private final PlanSaaSService planSaaSService;

    /**
     * GET /api/v1/planes
     * <p>Retorna la lista de planes SaaS activos. Accesible para cualquier usuario autenticado.</p>
     *
     * @return HTTP 200 con lista de {@link PlanSaaSDTO}
     */
    @GetMapping
    public ResponseEntity<List<PlanSaaSDTO>> listarPlanesActivos() {
        return ResponseEntity.ok(planSaaSService.listarPlanesActivos());
    }
}
