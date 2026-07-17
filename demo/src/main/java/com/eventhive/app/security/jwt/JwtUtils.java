package com.eventhive.app.security.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtUtils {

    private static final String CLAIM_TIPO  = "type";
    private static final String TIPO_ACCESS  = "access";
    private static final String TIPO_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarAccessToken(UserDetails userDetails) {
        String roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .claim(CLAIM_TIPO, TIPO_ACCESS)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    public String generarRefreshToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim(CLAIM_TIPO, TIPO_REFRESH)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(key())
                .compact();
    }

    public String getCorreoDesdeToken(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    private String getTipoDesdeToken(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token)
                .getPayload()
                .get(CLAIM_TIPO, String.class);
    }

    private boolean firmaValida(String token) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Token JWT inválido: {}", ex.getMessage());
            return false;
        }
    }

    // Bearer de rutas protegidas: exige claim type=access
    public boolean validarAccessToken(String token) {
        return firmaValida(token) && TIPO_ACCESS.equals(getTipoDesdeToken(token));
    }

    // Solo para /api/auth/refresh: exige claim type=refresh
    public boolean validarRefreshToken(String token) {
        return firmaValida(token) && TIPO_REFRESH.equals(getTipoDesdeToken(token));
    }
}