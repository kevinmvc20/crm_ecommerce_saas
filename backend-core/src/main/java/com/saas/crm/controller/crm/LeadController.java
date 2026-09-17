package com.saas.crm.controller.crm;

import com.saas.crm.dto.crm.LeadAsignarRequest;
import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.dto.crm.LeadCalificarRequest;
import com.saas.crm.dto.crm.LeadConvertirRequest;
import com.saas.crm.dto.crm.LeadCreateRequest;
import com.saas.crm.dto.crm.LeadResponse;
import com.saas.crm.service.LeadService;
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
 * Controlador REST para la gestión del ciclo de vida de Leads (prospectos) en
 * el CRM.
 *
 * <p>
 * Base URL: {@code /api/v1/crm/leads}
 * </p>
 * <p>
 * Acceso restringido a {@code ROLE_ADMIN_EMPRESA} y {@code ROLE_VENDEDOR}.
 * </p>
 *
 * <p>
 * El {@code tenantId} y el rol del usuario autenticado se extraen
 * automáticamente
 * del {@link Usuario} inyectado por Spring Security a través de
 * {@code @AuthenticationPrincipal}, garantizando el aislamiento multi-tenant
 * sin depender de path variables externos.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/crm/leads")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'VENDEDOR')")
public class LeadController {

    private final LeadService leadService;

    // ─── POST / ───────────────────────────────────────────────────────────────

    /**
     * Registra un nuevo Lead en estado {@code NUEVO}.
     *
     * <p>
     * POST {@code /api/v1/crm/leads}
     * </p>
     *
     * @param usuario usuario autenticado (principal del contexto de seguridad)
     * @param request payload con los datos del prospecto
     * @return HTTP 201 con el {@link LeadResponse} creado
     */
    @PostMapping
    public ResponseEntity<LeadResponse> crearLead(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody LeadCreateRequest request) {

        UUID tenantId = usuario.getTenant().getId();
        LeadResponse created = leadService.crearLead(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── GET / ────────────────────────────────────────────────────────────────

    /**
     * Lista los Leads del tenant con filtrado según el rol:
     * <ul>
     * <li>{@code ROLE_VENDEDOR}: solo sus leads asignados.</li>
     * <li>{@code ROLE_ADMIN_EMPRESA}: todos los leads del tenant.</li>
     * </ul>
     *
     * <p>
     * GET {@code /api/v1/crm/leads}
     * </p>
     *
     * @param usuario usuario autenticado
     * @return HTTP 200 con la lista de {@link LeadResponse}
     */
    @GetMapping
    public ResponseEntity<List<LeadResponse>> listarLeads(
            @AuthenticationPrincipal Usuario usuario) {

        UUID tenantId = usuario.getTenant().getId();
        UUID usuarioId = usuario.getId();
        String rol = usuario.getRol().getNombre(); // p.e. "ROLE_VENDEDOR"

        List<LeadResponse> leads = leadService.listarLeads(tenantId, usuarioId, rol);
        return ResponseEntity.ok(leads);
    }

    // ─── PUT /{id}/calificar ──────────────────────────────────────────────────

    /**
     * Califica un Lead actualizando su score y notas; pasa el estado a
     * {@code CALIFICADO}.
     *
     * <p>
     * PUT {@code /api/v1/crm/leads/{id}/calificar}
     * </p>
     *
     * @param usuario usuario autenticado
     * @param id      UUID del lead a calificar
     * @param request nuevos valores de score y notas
     * @return HTTP 200 con el {@link LeadResponse} actualizado
     */
    @PutMapping("/{id}/calificar")
    public ResponseEntity<LeadResponse> calificarLead(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id,
            @Valid @RequestBody LeadCalificarRequest request) {

        UUID tenantId = usuario.getTenant().getId();
        LeadResponse updated = leadService.calificarLead(tenantId, usuario, id, request);
        return ResponseEntity.ok(updated);
    }

    // ─── POST /{id}/convertir ─────────────────────────────────────────────────

    /**
     * Convierte un Lead calificado en un Cliente.
     *
     * <p>
     * POST {@code /api/v1/crm/leads/{id}/convertir}
     * </p>
     *
     * <p>
     * La operación es atómica: si falla la creación del cliente o la
     * actualización del lead, toda la transacción se revierte.
     * </p>
     *
     * @param usuario usuario autenticado
     * @param id      UUID del lead a convertir
     * @param request datos del nuevo cliente (CI/NIT y razón social)
     * @return HTTP 200 con el {@link LeadResponse} en estado {@code CONVERTIDO}
     */
    @PostMapping("/{id}/convertir")
    public ResponseEntity<LeadResponse> convertirACliente(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id,
            @Valid @RequestBody LeadConvertirRequest request) {

        UUID tenantId = usuario.getTenant().getId();
        LeadResponse converted = leadService.convertirACliente(tenantId, id, request);
        return ResponseEntity.ok(converted);
    }

    // ─── PATCH /{id}/asignar ──────────────────────────────────────────────────

    /**
     * Asigna o reasigna un vendedor a un Lead.
     * Operación restringida exclusivamente a ADMIN_EMPRESA.
     *
     * <p>
     * PATCH {@code /api/v1/crm/leads/{id}/asignar}
     * </p>
     *
     * @param usuario usuario autenticado
     * @param id      UUID del lead
     * @param request payload con el vendedorId (o null para desasignar)
     * @return HTTP 200 con el {@link LeadResponse} actualizado
     */
    @PatchMapping("/{id}/asignar")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<LeadResponse> asignarVendedor(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id,
            @RequestBody LeadAsignarRequest request) {

        UUID tenantId = usuario.getTenant().getId();
        LeadResponse updated = leadService.asignarVendedor(tenantId, id, request.vendedorId());
        return ResponseEntity.ok(updated);
    }
}
