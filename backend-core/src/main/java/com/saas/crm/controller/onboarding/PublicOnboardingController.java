package com.saas.crm.controller.onboarding;

import com.saas.crm.dto.onboarding.OnboardingRequest;
import com.saas.crm.dto.onboarding.OnboardingResponse;
import com.saas.crm.dto.saas.PlanSaaSDTO;
import com.saas.crm.service.PlanSaaSService;
import com.saas.crm.service.onboarding.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST publico para el flujo de Onboarding Self-Service.
 * Todos los endpoints bajo /api/v1/public/** estan exentos de autenticacion JWT.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/public/onboarding")
@RequiredArgsConstructor
public class PublicOnboardingController {

    private final OnboardingService onboardingService;
    private final PlanSaaSService   planSaaSService;

    /**
     * Registra una nueva empresa inquilina y procesa el cobro inicial.
     *
     * @return HTTP 201 Created con datos del tenant y transaccion de pago
     */
    @PostMapping("/register")
    public ResponseEntity<OnboardingResponse> registrarEmpresa(
            @Valid @RequestBody OnboardingRequest request) {
        log.info("[PublicOnboarding] POST /register | subdominio={}", request.subdominio());
        OnboardingResponse response = onboardingService.registrarEmpresaYAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Devuelve la lista de planes SaaS activos con precios y limites.
     *
     * @return HTTP 200 OK con lista de planes
     */
    @GetMapping("/planes")
    public ResponseEntity<List<PlanSaaSDTO>> listarPlanesPublicos() {
        log.info("[PublicOnboarding] GET /planes");
        return ResponseEntity.ok(planSaaSService.listarPlanesActivos());
    }
}