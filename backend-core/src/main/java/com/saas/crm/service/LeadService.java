package com.saas.crm.service;

import com.saas.crm.domain.entity.Cliente;
import com.saas.crm.domain.entity.InteraccionCRM;
import com.saas.crm.domain.entity.Lead;
import com.saas.crm.domain.entity.Tenant;
import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.domain.enums.EstadoLead;
import com.saas.crm.domain.enums.TipoInteraccionCRM;
import com.saas.crm.dto.crm.LeadCalificarRequest;
import com.saas.crm.dto.crm.LeadConvertirRequest;
import com.saas.crm.dto.crm.LeadCreateRequest;
import com.saas.crm.dto.crm.LeadResponse;
import com.saas.crm.repository.ClienteRepository;
import com.saas.crm.repository.InteraccionCRMRepository;
import com.saas.crm.repository.LeadRepository;
import com.saas.crm.repository.TenantRepository;
import com.saas.crm.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de negocio para la gestión del ciclo de vida de los Leads
 * (prospectos)
 * en el módulo CRM.
 *
 * <p>
 * Aplica aislamiento multi-tenant en todas las operaciones: cada consulta
 * y mutación está acotada al {@code tenantId} del usuario autenticado.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class LeadService {

    private static final String ROL_VENDEDOR = "ROLE_VENDEDOR";
    private static final String ROL_ADMIN_EMPRESA = "ROLE_ADMIN_EMPRESA";

    private final LeadRepository leadRepository;
    private final ClienteRepository clienteRepository;
    private final TenantRepository tenantRepository;
    private final UsuarioRepository usuarioRepository;
    private final InteraccionCRMRepository interaccionCRMRepository;

    // ─── Crear Lead ───────────────────────────────────────────────────────────

    /**
     * Registra un nuevo Lead en estado {@code NUEVO} para el tenant indicado.
     *
     * <p>
     * Si se provee {@code vendedorId} en el request, valida que el vendedor
     * pertenezca al mismo tenant antes de asignarlo.
     * </p>
     *
     * @param tenantId UUID del tenant autenticado
     * @param request  datos del nuevo lead
     * @return {@link LeadResponse} con los datos persistidos
     */
    @Transactional
    public LeadResponse crearLead(UUID tenantId, LeadCreateRequest request) {

        Tenant tenant = resolverTenant(tenantId);

        Lead lead = new Lead();
        lead.setTenant(tenant);
        lead.setNombre(request.nombre());
        lead.setEmail(request.email());
        lead.setTelefono(request.telefono());
        lead.setEstado(EstadoLead.NUEVO);
        lead.setScore(0);

        // Asignación opcional de vendedor
        if (request.vendedorId() != null) {
            Usuario vendedor = resolverVendedorDelTenant(tenantId, request.vendedorId());
            lead.setVendedorAsignado(vendedor);
        }

        return toResponse(leadRepository.save(lead));
    }

    // ─── Listar Leads ─────────────────────────────────────────────────────────

    /**
     * Lista los leads del tenant con filtrado según el rol del usuario solicitante.
     *
     * <ul>
     * <li>Si {@code rolSolicitante} es {@code ROLE_VENDEDOR}: devuelve solo los
     * leads asignados al {@code usuarioId} indicado.</li>
     * <li>Si {@code rolSolicitante} es {@code ROLE_ADMIN_EMPRESA}: devuelve todos
     * los leads del tenant.</li>
     * </ul>
     *
     * @param tenantId       UUID del tenant autenticado
     * @param usuarioId      UUID del usuario que realiza la solicitud
     * @param rolSolicitante nombre del rol del usuario (p.e. "ROLE_VENDEDOR")
     * @return lista de {@link LeadResponse}
     */
    @Transactional(readOnly = true)
    public List<LeadResponse> listarLeads(UUID tenantId, UUID usuarioId, String rolSolicitante) {

        List<Lead> leads;

        if (ROL_VENDEDOR.equals(rolSolicitante)) {
            // El vendedor solo ve sus propios leads asignados
            leads = leadRepository.findByTenantIdAndVendedorAsignadoId(tenantId, usuarioId);
        } else {
            // ADMIN_EMPRESA ve todos los leads del tenant
            leads = leadRepository.findByTenantId(tenantId);
        }

        return leads.stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Calificar Lead ───────────────────────────────────────────────────────

    /**
     * Actualiza el score y las notas del lead, avanzando su estado a
     * {@code CALIFICADO}. Registra automáticamente una entrada de auditoría
     * en la bitácora (InteraccionCRM tipo NOTA).
     *
     * <p>Reglas de negocio aplicadas en orden:</p>
     * <ol>
     *   <li>El lead debe tener vendedor asignado (HTTP 400).</li>
     *   <li>El lead debe haber sido contactado previamente; si su estado es
     *       {@code NUEVO} se rechaza la calificación (HTTP 400).</li>
     *   <li>El lead no debe estar en estado terminal (HTTP 400).</li>
     * </ol>
     *
     * @param tenantId          UUID del tenant autenticado
     * @param usuarioEjecutivo  usuario que realiza la calificación (puede ser null,
     *                          en cuyo caso se usa el vendedor asignado del lead)
     * @param leadId            UUID del lead a calificar
     * @param request           nuevos valores de score y notas
     * @return {@link LeadResponse} actualizado
     */
    @Transactional
    public LeadResponse calificarLead(UUID tenantId, Usuario usuarioEjecutivo, UUID leadId, LeadCalificarRequest request) {

        Lead lead = resolverLeadDelTenant(tenantId, leadId);

        // Guarda 1: el prospecto debe tener vendedor asignado antes de calificarse
        if (lead.getVendedorAsignado() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El prospecto debe tener un vendedor asignado antes de ser calificado.");
        }

        // Guarda 2 (candado secuencial): el prospecto debe haber sido contactado
        if (lead.getEstado() == EstadoLead.NUEVO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El prospecto debe ser contactado previamente antes de poder ser calificado.");
        }

        // Guarda 3: no permitir calificar estados terminales
        if (lead.getEstado() == EstadoLead.CONVERTIDO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El lead ya fue convertido a cliente y no puede calificarse nuevamente.");
        }
        if (lead.getEstado() == EstadoLead.DESCALIFICADO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El lead está descalificado y no puede modificarse.");
        }

        // Aplicar calificación
        lead.setScore(request.score());
        lead.setNotasCalificacion(request.notas());
        lead.setEstado(EstadoLead.CALIFICADO);
        Lead leadGuardado = leadRepository.save(lead);

        // Auditoría: registrar la calificación como NOTA en la bitácora
        Usuario ejecutorAuditoria = (usuarioEjecutivo != null)
                ? usuarioEjecutivo
                : lead.getVendedorAsignado();

        if (ejecutorAuditoria != null) {
            InteraccionCRM interaccion = new InteraccionCRM();
            interaccion.setTenant(lead.getTenant());
            interaccion.setLead(leadGuardado);
            interaccion.setEjecutivo(ejecutorAuditoria);
            interaccion.setTipo(TipoInteraccionCRM.NOTA);
            interaccion.setDescripcion(String.format(
                    "Calificación comercial: %d/100 pts. Notas: %s",
                    request.score(), request.notas()));
            interaccion.setFechaHora(LocalDateTime.now());
            interaccionCRMRepository.save(interaccion);
        }

        return toResponse(leadGuardado);
    }

    // ─── Convertir Lead a Cliente ─────────────────────────────────────────────

    /**
     * Convierte un Lead existente en un {@code Cliente}.
     *
     * <p>
     * Reglas de negocio aplicadas en orden:
     * <ol>
     * <li>El lead debe pertenecer al tenant (HTTP 404).</li>
     * <li>El lead no debe estar ya convertido o descalificado (HTTP 400).</li>
     * <li>El CI/NIT provisto no debe existir ya en el tenant (HTTP 409).</li>
     * <li>Se persiste el nuevo {@code Cliente} con {@code leadOrigen}
     * vinculado.</li>
     * <li>El estado del lead se actualiza a {@code CONVERTIDO}.</li>
     * </ol>
     * </p>
     *
     * @param tenantId UUID del tenant autenticado
     * @param leadId   UUID del lead a convertir
     * @param request  datos mínimos para crear el cliente
     * @return {@link LeadResponse} con el lead en estado {@code CONVERTIDO}
     */
    @Transactional
    public LeadResponse convertirACliente(UUID tenantId, UUID leadId, LeadConvertirRequest request) {

        Lead lead = resolverLeadDelTenant(tenantId, leadId);

        // Guarda: el prospecto debe tener vendedor asignado antes de convertirse
        if (lead.getVendedorAsignado() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El prospecto debe tener un vendedor asignado antes de ser convertido a cliente.");
        }

        // Guarda: el prospecto debe tener un score mínimo de 70 para ser convertido
        if (lead.getScore() == null || lead.getScore() < 70) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El prospecto debe tener un score mínimo de 70 para ser convertido a cliente.");
        }

        // 1. Validar estado del lead
        if (lead.getEstado() == EstadoLead.CONVERTIDO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El lead ya fue convertido previamente a cliente.");
        }
        if (lead.getEstado() == EstadoLead.DESCALIFICADO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se puede convertir un lead descalificado.");
        }

        // 2. Verificar unicidad de CI/NIT en el tenant
        if (clienteRepository.existsByTenantIdAndCiNit(tenantId, request.ciNit())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un cliente con el CI/NIT '%s' en este tenant.".formatted(request.ciNit()));
        }

        // 3. Crear el Cliente vinculando el lead de origen
        Cliente cliente = new Cliente();
        cliente.setTenant(lead.getTenant());
        cliente.setLeadOrigen(lead);
        cliente.setRazonSocialONombre(request.razonSocialONombre());
        cliente.setCiNit(request.ciNit());
        cliente.setEmail(lead.getEmail());
        cliente.setTelefono(lead.getTelefono());
        cliente.setActivo(true);
        clienteRepository.save(cliente);

        // 4. Marcar el lead como CONVERTIDO
        lead.setEstado(EstadoLead.CONVERTIDO);
        return toResponse(leadRepository.save(lead));
    }

    // ─── Asignar Vendedor ─────────────────────────────────────────────────────

    /**
     * Asigna o reasigna un vendedor a un Lead existente.
     * Si {@code vendedorId} es null, el lead queda en la bolsa general (sin
     * asignar).
     *
     * @param tenantId   UUID del tenant autenticado
     * @param leadId     UUID del lead a modificar
     * @param vendedorId UUID del nuevo vendedor (nullable)
     * @return {@link LeadResponse} actualizado
     */
    @Transactional
    public LeadResponse asignarVendedor(UUID tenantId, UUID leadId, UUID vendedorId) {

        Lead lead = resolverLeadDelTenant(tenantId, leadId);

        if (lead.getEstado() == EstadoLead.CONVERTIDO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El lead ya fue convertido a cliente y no puede reasignarse.");
        }
        if (lead.getEstado() == EstadoLead.DESCALIFICADO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El lead está descalificado y no puede reasignarse.");
        }

        if (vendedorId != null) {
            Usuario vendedor = resolverVendedorDelTenant(tenantId, vendedorId);
            lead.setVendedorAsignado(vendedor);
        } else {
            lead.setVendedorAsignado(null);
        }

        return toResponse(leadRepository.save(lead));
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    /** Resuelve el Tenant o lanza HTTP 404. */
    private Tenant resolverTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un tenant con id: " + tenantId));
    }

    /**
     * Resuelve el Lead asegurando que pertenezca al tenant indicado.
     * Lanza HTTP 404 si no existe o no pertenece al tenant.
     */
    private Lead resolverLeadDelTenant(UUID tenantId, UUID leadId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un lead con id: " + leadId));

        if (!lead.getTenant().getId().equals(tenantId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe un lead con id: " + leadId);
        }
        return lead;
    }

    /**
     * Resuelve el vendedor validando que pertenezca al tenant indicado.
     * Lanza HTTP 400 si el vendedor no existe o está en un tenant diferente.
     */
    private Usuario resolverVendedorDelTenant(UUID tenantId, UUID vendedorId) {
        Usuario vendedor = usuarioRepository.findById(vendedorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No existe un vendedor con id: " + vendedorId));

        if (vendedor.getTenant() == null || !vendedor.getTenant().getId().equals(tenantId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El vendedor indicado no pertenece a este tenant.");
        }
        return vendedor;
    }

    /** Convierte un {@link Lead} en su {@link LeadResponse} inmutable. */
    private LeadResponse toResponse(Lead lead) {
        Usuario vendedor = lead.getVendedorAsignado();
        return new LeadResponse(
                lead.getId(),
                lead.getTenant().getId(),
                vendedor != null ? vendedor.getId() : null,
                vendedor != null ? vendedor.getNombreCompleto() : null,
                lead.getNombre(),
                lead.getEmail(),
                lead.getTelefono(),
                lead.getEstado(),
                lead.getScore(),
                lead.getCreatedAt());
    }
}
