package com.saas.crm.controller.crm;

import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.dto.crm.InteraccionCreateRequest;
import com.saas.crm.dto.crm.InteraccionResponse;
import com.saas.crm.service.InteraccionCRMService;
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
 * Controlador REST para la Bitácora Comercial (InteraccionCRM).
 *
 * <p>Base URL: {@code /api/v1/crm/interacciones}</p>
 *
 * <p>Acceso restringido a {@code ROLE_ADMIN_EMPRESA} y {@code ROLE_VENDEDOR}.</p>
 *
 * <p>El {@code tenantId} y el {@code usuarioId} se extraen automáticamente del
 * {@link Usuario} inyectado por Spring Security mediante
 * {@code @AuthenticationPrincipal}, garantizando aislamiento multi-tenant sin
 * depender de path variables externos.</p>
 */
@RestController
@RequestMapping("/api/v1/crm/interacciones")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'VENDEDOR')")
public class InteraccionCRMController {

    private final InteraccionCRMService interaccionService;

    // ─── POST /lead/{leadId} ─────────────────────────────────────────────────

    /**
     * Registra una nueva actividad comercial sobre un Lead.
     *
     * <p>Si el lead estaba en estado {@code NUEVO}, la primera interacción
     * lo promueve automáticamente a {@code CONTACTADO}.</p>
     *
     * @param usuario  usuario autenticado (principal del contexto de seguridad)
     * @param leadId   UUID del Lead objetivo
     * @param request  payload con tipo y descripción de la interacción
     * @return HTTP 201 con el {@link InteraccionResponse} creado
     */
    @PostMapping("/lead/{leadId}")
    public ResponseEntity<InteraccionResponse> registrarInteraccionLead(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID leadId,
            @Valid @RequestBody InteraccionCreateRequest request) {

        UUID tenantId   = usuario.getTenant().getId();
        UUID ejecutivoId = usuario.getId();

        InteraccionResponse created =
                interaccionService.registrarInteraccionLead(tenantId, ejecutivoId, leadId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── GET /lead/{leadId} ──────────────────────────────────────────────────

    /**
     * Retorna el historial de interacciones de un Lead, del más reciente
     * al más antiguo.
     *
     * @param usuario usuario autenticado
     * @param leadId  UUID del Lead
     * @return HTTP 200 con la lista de {@link InteraccionResponse}
     */
    @GetMapping("/lead/{leadId}")
    public ResponseEntity<List<InteraccionResponse>> listarPorLead(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID leadId) {

        UUID tenantId = usuario.getTenant().getId();
        List<InteraccionResponse> lista = interaccionService.listarPorLead(tenantId, leadId);
        return ResponseEntity.ok(lista);
    }
}
