package com.saas.crm.service;

import com.saas.crm.domain.entity.Rol;
import com.saas.crm.domain.entity.Tenant;
import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.dto.usuario.UsuarioCreateRequest;
import com.saas.crm.dto.usuario.UsuarioResponse;
import com.saas.crm.repository.RolRepository;
import com.saas.crm.repository.TenantRepository;
import com.saas.crm.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de negocio para aprovisionar y consultar usuarios operacionales
 * (ROLE_ADMIN_EMPRESA y ROLE_VENDEDOR) dentro de un Tenant.
 */
@Service
@RequiredArgsConstructor
public class TenantUsuarioService {

    private static final String ROL_CLIENTE = "ROLE_CLIENTE";

    private final TenantRepository    tenantRepository;
    private final UsuarioRepository   usuarioRepository;
    private final RolRepository       rolRepository;
    private final PasswordEncoder     passwordEncoder;

    // ─── Crear usuario ───────────────────────────────────────────────────────

    /**
     * Aprovisiona un nuevo usuario operacional para el tenant indicado aplicando
     * las siguientes reglas de negocio en orden:
     * <ol>
     *   <li>El tenant debe existir (HTTP 404).</li>
     *   <li>El tenant debe estar activo (HTTP 400).</li>
     *   <li>El email no puede estar ya registrado globalmente (HTTP 409).</li>
     *   <li>El número de usuarios operacionales no puede superar el límite del plan (HTTP 400).</li>
     * </ol>
     *
     * @param tenantId UUID del tenant destino
     * @param request  datos del nuevo usuario
     * @return {@link UsuarioResponse} con el usuario persistido
     */
    @Transactional
    public UsuarioResponse crearUsuarioParaTenant(UUID tenantId, UsuarioCreateRequest request) {

        // 1. Tenant existe
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un tenant con id: " + tenantId
                ));

        // 2. Tenant activo
        if (!Boolean.TRUE.equals(tenant.getActivo())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El tenant '%s' se encuentra inactivo o suspendido.".formatted(tenant.getNombreComercial())
            );
        }

        // 3. Email único global
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El email '%s' ya está registrado en la plataforma.".formatted(request.email())
            );
        }

        // 4. Límite de usuarios del plan
        int limiteUsuarios = tenant.getPlanSaaS().getLimiteUsuarios();
        if (limiteUsuarios != -1) {
            long usuariosActuales = usuarioRepository
                    .countByTenantIdAndRolNombreNot(tenantId, ROL_CLIENTE);
            if (usuariosActuales >= limiteUsuarios) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Límite de usuarios del plan alcanzado (%d/%d).".formatted(usuariosActuales, limiteUsuarios)
                );
            }
        }

        // 5. Resolver el Rol (debe existir en la tabla rol; los roles del sistema se cargan en el seed V1)
        Rol rol = rolRepository.findByNombre(request.rolNombre())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El rol '%s' no existe en la plataforma.".formatted(request.rolNombre())
                ));

        // 6. Construir y persistir el Usuario
        Usuario usuario = new Usuario();
        usuario.setTenant(tenant);
        usuario.setRol(rol);
        usuario.setNombreCompleto(request.nombreCompleto());
        usuario.setEmail(request.email());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setTipoUsuario("OPERADOR_INTERNO");
        usuario.setActivo(true);
        // createdAt se inicializa automáticamente vía @PrePersist en Usuario

        Usuario saved = usuarioRepository.save(usuario);

        return toResponse(saved);
    }

    // ─── Listar usuarios ─────────────────────────────────────────────────────

    /**
     * Devuelve todos los usuarios operacionales registrados bajo el tenant indicado.
     *
     * @param tenantId UUID del tenant
     * @return lista de {@link UsuarioResponse}
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuariosPorTenant(UUID tenantId) {

        if (!tenantRepository.existsById(tenantId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe un tenant con id: " + tenantId
            );
        }

        return usuarioRepository.findByTenantId(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Mapeo interno ───────────────────────────────────────────────────────

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getTenant() != null ? usuario.getTenant().getId() : null,
                usuario.getEmail(),
                usuario.getRol().getNombre(),
                usuario.getActivo(),
                usuario.getCreatedAt()
        );
    }
}
