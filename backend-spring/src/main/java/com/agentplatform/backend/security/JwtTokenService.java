package com.agentplatform.backend.security;

import com.agentplatform.backend.common.security.CurrentUser;
import com.agentplatform.backend.user.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT 签发和解析服务。
 */
@Service
public class JwtTokenService {

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtTokenService(
            @Value("${app.security.jwt-secret}") String jwtSecret,
            @Value("${app.security.token-expiration-minutes:120}")
            long expirationMinutes
    ) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String createToken(CurrentUser user) {
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plusSeconds(expirationMinutes * 60);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.userId())
                .claim("tenantId", user.tenantId())
                .claim("username", user.username())
                .claim("displayName", user.displayName())
                .claim("roles", user.roles().stream().map(Enum::name).toList())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    public CurrentUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        List<?> roleValues = claims.get("roles", List.class);
        Set<UserRole> roles = roleValues == null
                ? Set.of(UserRole.VIEWER)
                : roleValues.stream()
                .map(String::valueOf)
                .map(UserRole::valueOf)
                .collect(Collectors.toUnmodifiableSet());

        return new CurrentUser(
                claims.getSubject(),
                claims.get("tenantId", String.class),
                claims.get("username", String.class),
                claims.get("displayName", String.class),
                roles
        );
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
}
