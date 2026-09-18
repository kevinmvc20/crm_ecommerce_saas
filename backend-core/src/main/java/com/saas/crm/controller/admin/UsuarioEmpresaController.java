package com.saas.crm.controller.admin;

import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.dto.usuario.UsuarioCreateRequest;
import com.saas.crm.dto.usuario.UsuarioResponse;
import com.saas.crm.service.UsuarioEmpresaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
@RequiredArgsConstructor
public class UsuarioEmpresaController {

    private final UsuarioEmpresaService usuarioEmpresaService;

    @GetMapping
    public List<UsuarioResponse> listarUsuarios(
            @AuthenticationPrincipal Usuario usuarioAuth) {
        return usuarioEmpresaService.listarUsuariosPorTenant(usuarioAuth.getTenant().getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crearVendedor(
            @AuthenticationPrincipal Usuario usuarioAuth,
            @Valid @RequestBody UsuarioCreateRequest request) {
        return usuarioEmpresaService.crearVendedor(usuarioAuth.getTenant().getId(), request);
    }

    @PatchMapping("/{id}/toggle-activo")
    public UsuarioResponse toggleActivo(
            @AuthenticationPrincipal Usuario usuarioAuth,
            @PathVariable UUID id) {
        return usuarioEmpresaService.cambiarEstadoActivo(usuarioAuth.getTenant().getId(), id, usuarioAuth);
    }
}
