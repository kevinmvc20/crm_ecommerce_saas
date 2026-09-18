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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioEmpresaService {

    private final UsuarioRepository usuarioRepository;
    private final TenantRepository tenantRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuariosPorTenant(UUID tenantId) {
        return usuarioRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UsuarioResponse crearVendedor(UUID tenantId, UsuarioCreateRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo electrónico ya se encuentra registrado.");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant no encontrado."));

        validarLimiteUsuarios(tenant);

        Rol rolVendedor = rolRepository.findByNombre("ROLE_VENDEDOR")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Rol ROLE_VENDEDOR no encontrado en el sistema."));

        Usuario usuario = new Usuario();
        usuario.setTenant(tenant);
        usuario.setRol(rolVendedor);
        usuario.setEmail(request.email());
        usuario.setNombreCompleto(request.nombreCompleto());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setTipoUsuario("INTERNO");
        usuario.setActivo(true);
        usuario.setCreatedAt(LocalDateTime.now());

        usuario = usuarioRepository.save(usuario);
        return mapToResponse(usuario);
    }

    @Transactional
    public UsuarioResponse cambiarEstadoActivo(UUID tenantId, UUID usuarioId, Usuario usuarioAuth) {
        if (usuarioId.equals(usuarioAuth.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes desactivar tu propia cuenta de administrador.");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        if (usuario.getTenant() == null || !usuario.getTenant().getId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado en el tenant.");
        }

        boolean nuevoEstado = !usuario.getActivo();

        if (nuevoEstado) {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant no encontrado."));
            validarLimiteUsuarios(tenant);
        }

        usuario.setActivo(nuevoEstado);
        usuario = usuarioRepository.save(usuario);
        return mapToResponse(usuario);
    }

    private void validarLimiteUsuarios(Tenant tenant) {
        if (tenant.getPlanSaaS() != null) {
            long usuariosActivos = usuarioRepository.countByTenantIdAndActivoTrue(tenant.getId());
            if (usuariosActivos >= tenant.getPlanSaaS().getLimiteUsuarios()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se ha alcanzado el límite de usuarios permitidos en su plan actual (" + tenant.getPlanSaaS().getLimiteUsuarios() + ").");
            }
        }
    }

    private UsuarioResponse mapToResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getTenant() != null ? usuario.getTenant().getId() : null,
                usuario.getNombreCompleto(),
                usuario.getEmail(),
                usuario.getRol().getNombre(),
                usuario.getActivo(),
                usuario.getCreatedAt()
        );
    }
}
