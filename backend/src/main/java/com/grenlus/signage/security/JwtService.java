package com.grenlus.signage.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Genera y valida los tokens JWT del panel.
 *
 * El token es autocontenido y firmado: el servidor no guarda sesiones, valida
 * la firma en cada request. Por eso alcanza con que el secreto sea el mismo, y
 * por eso tambien un token no se puede revocar antes de que expire.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long duracionMs;

    public JwtService(@Value("${signage.jwt.secreto}") String secreto,
                      @Value("${signage.jwt.duracion-ms}") long duracionMs) {
        // HS256 exige una clave de al menos 256 bits: 32 caracteres.
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.duracionMs = duracionMs;
    }

    public String generar(UserDetails usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getUsername())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusMillis(duracionMs)))
                .signWith(clave)
                .compact();
    }

    public String emailDe(String token) {
        return claims(token).getSubject();
    }

    public boolean esValido(String token, UserDetails usuario) {
        try {
            Claims claims = claims(token);
            return claims.getSubject().equals(usuario.getUsername())
                    && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            // Firma invalida, token manipulado o expirado: no autentica.
            return false;
        }
    }

    public long duracionMs() {
        return duracionMs;
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
