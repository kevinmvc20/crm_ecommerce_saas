package com.saas.crm.controller.auth;

import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.dto.auth.AuthResponse;
import com.saas.crm.dto.auth.LoginRequest;
import com.saas.crm.repository.UsuarioRepository;
import com.saas.crm.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para el flujo de autenticación.
 * No requiere estar autenticado para acceder al endpoint /login.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Autentica al usuario con email y contraseña.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Busca el usuario por email.</li>
     *   <li>Verifica que esté activo.</li>
     *   <li>Compara la contraseña con BCrypt.</li>
     *   <li>Genera el JWT con claims de rol, tenantId y email.</li>
     *   <li>Devuelve HTTP 200 con {@link AuthResponse}.</li>
     * </ol>
     *
     * @param request DTO con email y password (validado con Bean Validation)
     * @return 200 OK con AuthResponse, o 401 Unauthorized si las credenciales fallan
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

        // 1. Buscar usuario por email
        Usuario usuario = usuarioRepository
                .findByEmail(request.email())
                .orElse(null);

        // 2. Validar existencia, estado activo y contraseña
        if (usuario == null
                || !usuario.isEnabled()
                || !passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 3. Extraer datos del tenant (nullable para superadmin)
        UUID tenantId = usuario.getTenant() != null ? usuario.getTenant().getId() : null;
        String nombreTenant = usuario.getTenant() != null ? usuario.getTenant().getNombreComercial() : null;

        // 4. Generar token JWT
        String token = jwtService.generateToken(
                usuario.getEmail(),
                usuario.getRol().getNombre(),
                tenantId
        );

        // 5. Construir y devolver la respuesta
        AuthResponse response = new AuthResponse(
                token,
                usuario.getEmail(),
                usuario.getRol().getNombre(),
                tenantId,
                nombreTenant
        );

        return ResponseEntity.ok(response);
    }
}
