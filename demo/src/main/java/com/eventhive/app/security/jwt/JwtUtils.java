package com.eventhive.app.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtUtils {

    public static final String CLAIM_TIPO   = "type";
    public static final String TIPO_ACCESS  = "access";
    public static final String TIPO_REFRESH = "refresh";

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
        try {
            return Jwts.parser().verifyWith(key()).build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (ExpiredJwtException ex) {
            return ex.getClaims().getSubject();
        }
    }

    public String getTipoDesdeToken(String token) {
        if (!StringUtils.hasText(token)) return null;
        try {
            return Jwts.parser().verifyWith(key()).build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get(CLAIM_TIPO, String.class);
        } catch (ExpiredJwtException ex) {
            return ex.getClaims().get(CLAIM_TIPO, String.class);
        } catch (Exception ex) {
            return null;
        }
    }

    public boolean esTokenExpirado(String token) {
        if (!StringUtils.hasText(token)) return false;
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
            return false;
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean firmaValida(String token) {
        if (!StringUtils.hasText(token)) return false;
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("Token JWT expirado: {}", ex.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Token JWT con formato o firma inválida: {}", ex.getMessage());
            return false;
        }
    }

    // Bearer de rutas protegidas: exige firma válida, no haber expirado y claim type=access
    public boolean validarAccessToken(String token) {
        return firmaValida(token) && TIPO_ACCESS.equals(getTipoDesdeToken(token));
    }

    // Solo para /api/auth/refresh: exige firma válida, no haber expirado y claim type=refresh
    public boolean validarRefreshToken(String token) {
        return firmaValida(token) && TIPO_REFRESH.equals(getTipoDesdeToken(token));
    }
}