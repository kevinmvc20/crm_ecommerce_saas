package com.saas.crm.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Servicio responsable de la generación, validación y extracción de claims
 * de tokens JWT firmados con HMAC-SHA256.
 */
@Service
public class JwtService {

    /** Clave secreta inyectada desde application.properties. */
    private final SecretKey signingKey;

    /** Tiempo de expiración del token en milisegundos (default: 24h). */
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret:XDef4ultS3cr3tK3yForD3v0nlyN0tF0rPr0duction!2024}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // ─── Generación ───────────────────────────────────────────────────────────

    /**
     * Genera un JWT firmado que incluye los claims de rol, tenantId y email.
     *
     * @param email    sujeto del token
     * @param rol      nombre del rol del usuario (ej: ROLE_SUPER_ADMIN)
     * @param tenantId UUID del tenant (puede ser null para superadmin)
     * @return token JWT serializado como String
     */
    public String generateToken(String email, String rol, UUID tenantId) {
        Map<String, Object> extraClaims = Map.of(
                "rol",      rol,
                "tenantId", tenantId != null ? tenantId.toString() : "",
                "email",    email
        );
        return buildToken(email, extraClaims);
    }

    private String buildToken(String subject, Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    // ─── Validación ───────────────────────────────────────────────────────────

    /**
     * Valida que el token pertenezca al usuario y no haya expirado.
     *
     * @param token       JWT a validar
     * @param userDetails usuario cargado desde BD
     * @return true si el token es válido
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ─── Extracción de claims ─────────────────────────────────────────────────

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRol(String token) {
        return extractClaim(token, claims -> claims.get("rol", String.class));
    }

    public String extractTenantId(String token) {
        return extractClaim(token, claims -> claims.get("tenantId", String.class));
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
