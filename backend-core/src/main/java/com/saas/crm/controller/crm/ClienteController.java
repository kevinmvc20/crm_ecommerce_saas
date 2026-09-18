package com.saas.crm.controller.crm;

import com.saas.crm.domain.entity.Cliente;
import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.dto.crm.ClienteResponse;
import com.saas.crm.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la consulta de Clientes en el módulo CRM.
 *
 * <p>Base URL: {@code /api/v1/crm/clientes}</p>
 * <p>Acceso restringido a {@code ROLE_ADMIN_EMPRESA} y {@code ROLE_VENDEDOR}.</p>
 */
@RestController
@RequestMapping("/api/v1/crm/clientes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'VENDEDOR')")
public class ClienteController {

    private final ClienteRepository clienteRepository;

    // ─── GET / ────────────────────────────────────────────────────────────────

    /**
     * Lista todos los clientes activos del tenant autenticado.
     * Utilizado principalmente para poblar el selector de clientes en el Kanban.
     *
     * <p>GET {@code /api/v1/crm/clientes}</p>
     *
     * @param usuario usuario autenticado
     * @return HTTP 200 con la lista de {@link ClienteResponse}
     */
    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listarClientes(
            @AuthenticationPrincipal Usuario usuario) {

        UUID tenantId = usuario.getTenant().getId();
        List<ClienteResponse> clientes = clienteRepository
                .findByTenantId(tenantId)
                .stream()
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(clientes);
    }

    /** Convierte un {@link Cliente} en su {@link ClienteResponse} inmutable. */
    private ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getRazonSocialONombre(),
                cliente.getCiNit(),
                cliente.getEmail()
        );
    }
}
