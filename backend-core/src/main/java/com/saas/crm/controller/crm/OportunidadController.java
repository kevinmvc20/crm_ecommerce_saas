package com.saas.crm.controller.crm;

import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.dto.crm.CambioEtapaRequest;
import com.saas.crm.dto.crm.CerrarPerdidaRequest;
import com.saas.crm.dto.crm.OportunidadCreateRequest;
import com.saas.crm.dto.crm.OportunidadResponse;
import com.saas.crm.service.OportunidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión del pipeline de ventas (Oportunidades Comerciales).
 *
 * <p>Base URL: {@code /api/v1/crm/oportunidades}</p>
 * <p>Acceso restringido a {@code ROLE_ADMIN_EMPRESA} y {@code ROLE_VENDEDOR}.</p>
 *
 * <p>El {@code tenantId} y el vendedor autenticado se extraen automáticamente
 * del {@link Usuario} inyectado por Spring Security mediante
 * {@code @AuthenticationPrincipal}, garantizando el aislamiento multi-tenant.</p>
 */
@RestController
@RequestMapping("/api/v1/crm/oportunidades")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'VENDEDOR')")
public class OportunidadController {

    private final OportunidadService oportunidadService;

    // ─── GET / ────────────────────────────────────────────────────────────────

    /**
     * Lista todas las oportunidades del tenant autenticado.
     *
     * <p>GET {@code /api/v1/crm/oportunidades}</p>
     *
     * @param usuario usuario autenticado (principal del contexto de seguridad)
     * @return HTTP 200 con la lista de {@link OportunidadResponse}
     */
    @GetMapping
    public ResponseEntity<List<OportunidadResponse>> listarTodas(
            @AuthenticationPrincipal Usuario usuario) {

        UUID tenantId = usuario.getTenant().getId();
        return ResponseEntity.ok(oportunidadService.listarTodas(tenantId));
    }

    // ─── POST / ───────────────────────────────────────────────────────────────

    /**
     * Registra una nueva Oportunidad en etapa CALIFICACION.
     *
     * <p>POST {@code /api/v1/crm/oportunidades}</p>
     *
     * @param usuario usuario autenticado (será el vendedor asignado)
     * @param request datos de la nueva oportunidad
     * @return HTTP 201 con el {@link OportunidadResponse} creado
     */
    @PostMapping
    public ResponseEntity<OportunidadResponse> crear(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody OportunidadCreateRequest request) {

        UUID tenantId = usuario.getTenant().getId();
        OportunidadResponse created = oportunidadService.crear(tenantId, usuario, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── PATCH /{id}/etapa ────────────────────────────────────────────────────

    /**
     * Cambia la etapa activa de una oportunidad (CALIFICACION → PROPUESTA → NEGOCIACION).
     * No acepta GANADA ni PERDIDA; use los endpoints especializados.
     *
     * <p>PATCH {@code /api/v1/crm/oportunidades/{id}/etapa}</p>
     *
     * @param usuario usuario autenticado
     * @param id      UUID de la oportunidad
     * @param request nueva etapa del pipeline
     * @return HTTP 200 con el {@link OportunidadResponse} actualizado
     */
    @PatchMapping("/{id}/etapa")
    public ResponseEntity<OportunidadResponse> cambiarEtapa(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id,
            @Valid @RequestBody CambioEtapaRequest request) {

        UUID tenantId = usuario.getTenant().getId();
        return ResponseEntity.ok(oportunidadService.cambiarEtapa(tenantId, id, request));
    }

    // ─── PATCH /{id}/ganada ───────────────────────────────────────────────────

    /**
     * Cierra la oportunidad como GANADA (probabilidad = 100 %).
     * Una vez cerrada, no puede modificarse.
     *
     * <p>PATCH {@code /api/v1/crm/oportunidades/{id}/ganada}</p>
     *
     * @param usuario usuario autenticado
     * @param id      UUID de la oportunidad
     * @return HTTP 200 con el {@link OportunidadResponse} actualizado
     */
    @PatchMapping("/{id}/ganada")
    public ResponseEntity<OportunidadResponse> cerrarGanada(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id) {

        UUID tenantId = usuario.getTenant().getId();
        return ResponseEntity.ok(oportunidadService.cerrarGanada(tenantId, id));
    }

    // ─── PATCH /{id}/perdida ──────────────────────────────────────────────────

    /**
     * Cierra la oportunidad como PERDIDA (probabilidad = 0 %).
     * Requiere motivo obligatorio para garantizar trazabilidad comercial.
     * Una vez cerrada, no puede modificarse.
     *
     * <p>PATCH {@code /api/v1/crm/oportunidades/{id}/perdida}</p>
     *
     * @param usuario usuario autenticado
     * @param id      UUID de la oportunidad
     * @param request motivo de pérdida
     * @return HTTP 200 con el {@link OportunidadResponse} actualizado
     */
    @PatchMapping("/{id}/perdida")
    public ResponseEntity<OportunidadResponse> cerrarPerdida(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id,
            @Valid @RequestBody CerrarPerdidaRequest request) {

        UUID tenantId = usuario.getTenant().getId();
        return ResponseEntity.ok(oportunidadService.cerrarPerdida(tenantId, id, request));
    }
}
