package com.ktb.community.purejwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class PureJwtUtil {
    private final Key key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public PureJwtUtil(
            @Value("${JWT_SECRET}") String secretKey,
            @Value("${jwt.expiration.access}") long accessTokenExpiration,
            @Value("${jwt.expiration.refresh}") long refreshTokenExpiration) {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        // JWT의 서명 키는 byte[]기반 HMAC키이므로 byte[]를 넣어야함
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // 액세스 토큰 발급 함수
    public String generateAccessToken(Long userId, String email) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusMillis(accessTokenExpiration)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 리프레시 토큰 발급 함수 (보안을 위해 email 제거)
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("typ", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusMillis(refreshTokenExpiration)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // JWT유효성 검증 및, Claim 분석
    public Jws<Claims> parse(String jwt) {
        try {
            return Jwts
                    .parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwt);
        } catch (SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token : ", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token : ", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty", e);
        }
        return null;
    }

    public LocalDateTime getExpirationFromToken(String token) {
        return parse(token)
                .getBody()
                .getExpiration()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

}
