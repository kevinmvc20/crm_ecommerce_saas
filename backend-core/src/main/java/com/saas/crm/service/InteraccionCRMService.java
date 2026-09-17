package com.saas.crm.service;

import com.saas.crm.domain.entity.InteraccionCRM;
import com.saas.crm.domain.entity.Lead;
import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.domain.enums.EstadoLead;
import com.saas.crm.dto.crm.InteraccionCreateRequest;
import com.saas.crm.dto.crm.InteraccionResponse;
import com.saas.crm.repository.InteraccionCRMRepository;
import com.saas.crm.repository.LeadRepository;
import com.saas.crm.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de negocio para la gestión de la Bitácora Comercial (InteraccionCRM).
 *
 * <p>Implementa la regla crítica: la primera interacción registrada sobre un
 * Lead en estado {@code NUEVO} promueve automáticamente su estado a
 * {@code CONTACTADO}.</p>
 */
@Service
@RequiredArgsConstructor
public class InteraccionCRMService {

    private final InteraccionCRMRepository interaccionRepo;
    private final LeadRepository           leadRepo;
    private final UsuarioRepository        usuarioRepo;

    // ─── Registrar interacción sobre un Lead ─────────────────────────────────

    /**
     * Registra una nueva actividad comercial sobre un Lead y aplica la
     * transición de estado NUEVO → CONTACTADO si corresponde.
     *
     * @param tenantId           UUID del tenant (extraído del principal autenticado)
     * @param usuarioEjecutivoId UUID del ejecutivo que registra la interacción
     * @param leadId             UUID del Lead objetivo
     * @param request            Datos de la interacción (tipo + descripción)
     * @return {@link InteraccionResponse} con la interacción persistida
     * @throws ResponseStatusException 404 si el lead no existe o no pertenece al tenant
     * @throws ResponseStatusException 400 si el usuario ejecutivo es inválido para el tenant
     */
    @Transactional
    public InteraccionResponse registrarInteraccionLead(
            UUID tenantId,
            UUID usuarioEjecutivoId,
            UUID leadId,
            InteraccionCreateRequest request) {

        // 1. Validar que el Lead exista y pertenezca al tenant
        Lead lead = leadRepo.findById(leadId)
                .filter(l -> l.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Lead no encontrado o no pertenece a este tenant."));

        // 2. Resolver el ejecutivo y validar pertenencia al tenant
        Usuario ejecutivo = usuarioRepo.findById(usuarioEjecutivoId)
                .filter(u -> u.getTenant() != null &&
                             u.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El usuario ejecutivo no es válido para este tenant."));

        // 3. Crear y persistir la interacción
        InteraccionCRM interaccion = new InteraccionCRM();
        interaccion.setTenant(lead.getTenant());
        interaccion.setLead(lead);
        interaccion.setEjecutivo(ejecutivo);
        interaccion.setTipo(request.tipo());
        interaccion.setDescripcion(request.descripcion());
        InteraccionCRM guardada = interaccionRepo.save(interaccion);

        // 4. REGLA CLAVE: primera interacción promueve NUEVO → CONTACTADO
        if (lead.getEstado() == EstadoLead.NUEVO) {
            lead.setEstado(EstadoLead.CONTACTADO);
            leadRepo.save(lead);
        }

        return toResponse(guardada);
    }

    // ─── Listar interacciones de un Lead ──────────────────────────────────────

    /**
     * Devuelve el historial de interacciones de un Lead, del más reciente
     * al más antiguo, validando pertenencia al tenant.
     *
     * @param tenantId UUID del tenant
     * @param leadId   UUID del Lead
     * @return lista de {@link InteraccionResponse} ordenada desc por fecha
     * @throws ResponseStatusException 404 si el lead no existe o no pertenece al tenant
     */
    @Transactional(readOnly = true)
    public List<InteraccionResponse> listarPorLead(UUID tenantId, UUID leadId) {

        // Validar pertenencia del lead al tenant
        leadRepo.findById(leadId)
                .filter(l -> l.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Lead no encontrado o no pertenece a este tenant."));

        return interaccionRepo
                .findByTenantIdAndLeadIdOrderByFechaHoraDesc(tenantId, leadId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Mapper interno ──────────────────────────────────────────────────────

    private InteraccionResponse toResponse(InteraccionCRM i) {
        return new InteraccionResponse(
                i.getId(),
                i.getTenant().getId(),
                i.getLead()    != null ? i.getLead().getId()    : null,
                i.getCliente() != null ? i.getCliente().getId() : null,
                i.getEjecutivo().getId(),
                i.getEjecutivo().getNombreCompleto(),
                i.getTipo(),
                i.getDescripcion(),
                i.getFechaHora()
        );
    }
}
