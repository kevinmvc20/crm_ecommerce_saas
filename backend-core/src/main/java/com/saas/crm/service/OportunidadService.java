package com.saas.crm.service;

import com.saas.crm.domain.entity.Cliente;
import com.saas.crm.domain.entity.Oportunidad;
import com.saas.crm.domain.entity.Tenant;
import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.dto.crm.CambioEtapaRequest;
import com.saas.crm.dto.crm.CerrarPerdidaRequest;
import com.saas.crm.dto.crm.OportunidadCreateRequest;
import com.saas.crm.dto.crm.OportunidadResponse;
import com.saas.crm.repository.ClienteRepository;
import com.saas.crm.repository.OportunidadRepository;
import com.saas.crm.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de negocio para la gestión del ciclo de vida de las Oportunidades
 * Comerciales en el pipeline de ventas (Kanban).
 *
 * <p>Aplica aislamiento multi-tenant en todas las operaciones.</p>
 */
@Service
@RequiredArgsConstructor
public class OportunidadService {

    private final OportunidadRepository oportunidadRepository;
    private final ClienteRepository clienteRepository;
    private final TenantRepository tenantRepository;

    // ─── Crear ───────────────────────────────────────────────────────────────

    /**
     * Crea una nueva oportunidad para el tenant autenticado.
     * La etapa inicial siempre es CALIFICACION.
     *
     * @param tenantId UUID del tenant autenticado
     * @param vendedor usuario vendedor autenticado (será el responsable)
     * @param request  datos de la nueva oportunidad
     * @return {@link OportunidadResponse} con los datos persistidos
     */
    @Transactional
    public OportunidadResponse crear(UUID tenantId, Usuario vendedor,
                                     OportunidadCreateRequest request) {
        Tenant tenant = resolverTenant(tenantId);
        Cliente cliente = resolverClienteDelTenant(tenantId, request.clienteId());

        Oportunidad op = new Oportunidad();
        op.setTenant(tenant);
        op.setCliente(cliente);
        op.setVendedor(vendedor);
        op.setTitulo(request.nombre());
        op.setMontoEstimado(request.montoEstimado());
        op.setProbabilidad(request.probabilidad());
        op.setFechaCierreEstimada(request.fechaCierreEsperada());
        // etapa y timestamps se inicializan en @PrePersist

        return toResponse(oportunidadRepository.save(op));
    }

    // ─── Listar ──────────────────────────────────────────────────────────────

    /**
     * Lista todas las oportunidades del tenant en orden cronológico inverso.
     *
     * @param tenantId UUID del tenant autenticado
     * @return lista de {@link OportunidadResponse}
     */
    @Transactional(readOnly = true)
    public List<OportunidadResponse> listarTodas(UUID tenantId) {
        return oportunidadRepository
                .findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Cambiar Etapa ───────────────────────────────────────────────────────

    /**
     * Cambia la etapa de una oportunidad a CALIFICACION, PROPUESTA o NEGOCIACION.
     * Para cerrar como GANADA o PERDIDA, use los endpoints especializados.
     *
     * @param tenantId UUID del tenant autenticado
     * @param id       UUID de la oportunidad
     * @param request  nueva etapa (CALIFICACION, PROPUESTA o NEGOCIACION)
     * @return {@link OportunidadResponse} actualizada
     */
    @Transactional
    public OportunidadResponse cambiarEtapa(UUID tenantId, UUID id,
                                             CambioEtapaRequest request) {
        Oportunidad op = resolverOportunidadDelTenant(tenantId, id);
        // avanzarEtapa valida que no sea terminal ni apunte a GANADA/PERDIDA
        op.avanzarEtapa(request.etapa());
        return toResponse(oportunidadRepository.save(op));
    }

    // ─── Cerrar Ganada ───────────────────────────────────────────────────────

    /**
     * Cierra la oportunidad como GANADA (probabilidad = 100 %).
     *
     * @param tenantId UUID del tenant autenticado
     * @param id       UUID de la oportunidad
     * @return {@link OportunidadResponse} actualizada
     */
    @Transactional
    public OportunidadResponse cerrarGanada(UUID tenantId, UUID id) {
        Oportunidad op = resolverOportunidadDelTenant(tenantId, id);
        op.cerrarGanada();
        return toResponse(oportunidadRepository.save(op));
    }

    // ─── Cerrar Perdida ──────────────────────────────────────────────────────

    /**
     * Cierra la oportunidad como PERDIDA (probabilidad = 0 %) con motivo obligatorio.
     *
     * @param tenantId UUID del tenant autenticado
     * @param id       UUID de la oportunidad
     * @param request  motivo de pérdida (requerido)
     * @return {@link OportunidadResponse} actualizada
     */
    @Transactional
    public OportunidadResponse cerrarPerdida(UUID tenantId, UUID id,
                                              CerrarPerdidaRequest request) {
        Oportunidad op = resolverOportunidadDelTenant(tenantId, id);
        op.cerrarPerdida(request.motivo());
        return toResponse(oportunidadRepository.save(op));
    }

    // ─── Helpers privados ────────────────────────────────────────────────────

    /** Resuelve el Tenant o lanza HTTP 404. */
    private Tenant resolverTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un tenant con id: " + tenantId));
    }

    /**
     * Resuelve el Cliente verificando que pertenezca al tenant indicado.
     * Lanza HTTP 404 si no existe o no pertenece al tenant.
     */
    private Cliente resolverClienteDelTenant(UUID tenantId, UUID clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un cliente con id: " + clienteId));

        if (!cliente.getTenant().getId().equals(tenantId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe un cliente con id: " + clienteId);
        }
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El cliente indicado está inactivo y no puede asociarse a una oportunidad.");
        }
        return cliente;
    }

    /**
     * Resuelve la Oportunidad verificando que pertenezca al tenant indicado.
     * Lanza HTTP 404 si no existe o no pertenece al tenant.
     */
    private Oportunidad resolverOportunidadDelTenant(UUID tenantId, UUID id) {
        Oportunidad op = oportunidadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe una oportunidad con id: " + id));

        if (!op.getTenant().getId().equals(tenantId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe una oportunidad con id: " + id);
        }
        return op;
    }

    /** Convierte una {@link Oportunidad} en su {@link OportunidadResponse} inmutable. */
    private OportunidadResponse toResponse(Oportunidad op) {
        return new OportunidadResponse(
                op.getId(),
                op.getCliente().getId(),
                op.getCliente().getRazonSocialONombre(),
                op.getVendedor().getId(),
                op.getVendedor().getNombreCompleto(),
                op.getTitulo(),           // alias: nombre
                op.getMontoEstimado(),
                op.getProbabilidad(),
                op.getEtapa(),
                op.getMotivoCierre(),     // alias: motivoPerdida
                op.getFechaCierreEstimada(),
                op.getCreatedAt()
        );
    }
}
