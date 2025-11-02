package com.ktb.community.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.community.dto.redis.StoredTokenDto;
import com.ktb.community.dto.response.ReIssueRefreshTokenDto;
import com.ktb.community.entity.User;
import com.ktb.community.exception.custom.InvalidRefreshTokenException;
import com.ktb.community.purejwt.PureJwtUtil;
import com.ktb.community.redis.RedisSingleDataServiceImpl;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Transactional
public class RefreshTokenService {
    private final RedisSingleDataServiceImpl redis;
    private final PureJwtUtil pureJwtUtil;
    private final ObjectMapper objectMapper;

    public RefreshTokenService(RedisSingleDataServiceImpl redis, PureJwtUtil pureJwtUtil, ObjectMapper objectMapper) {
        this.redis = redis;
        this.pureJwtUtil = pureJwtUtil;
        this.objectMapper = objectMapper;
    }

    public void saveRefreshToken(String token, User user, LocalDateTime expiredAt) {
        saveRefreshTokenWithEmail(token, user.getId(), user.getEmail(), expiredAt);
    }

    public void saveRefreshTokenWithEmail(String token, Long userId, String email, LocalDateTime expiredAt) {
        String key = "refresh_token:" + userId;
        long ttlSeconds = Duration.between(LocalDateTime.now(), expiredAt).getSeconds();

        StoredTokenDto storedToken = StoredTokenDto.builder()
                .refreshToken(token)
                .email(email)
                .build();

        try {
            String jsonValue = objectMapper.writeValueAsString(storedToken);
            redis.setSingleData(key, jsonValue, Duration.ofSeconds(ttlSeconds));
        } catch (JsonProcessingException e) {
            throw new InvalidRefreshTokenException("Failed to serialize token");
        }
    }


    public Boolean existByToken(String token) {
        var jws = pureJwtUtil.parse(token);
        if (jws == null) return false;

        long userId = Long.parseLong(jws.getBody().getSubject());

        String key = "refresh_token:" + userId;
        String jsonValue = redis.getSingleData(key);

        if (jsonValue == null || jsonValue.isEmpty()) return false;

        try {
            StoredTokenDto storedToken = objectMapper.readValue(jsonValue, StoredTokenDto.class);
            return token.equals(storedToken.getRefreshToken());
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private Long getUserIdFromToken(String token) {
        var jws = pureJwtUtil.parse(token);
        if (jws == null) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }
        return Long.parseLong(jws.getBody().getSubject());
    }

    private StoredTokenDto getStoredTokenFromRedis(Long userId) {
        String key = "refresh_token:" + userId;
        String jsonValue = redis.getSingleData(key);

        if (jsonValue == null || jsonValue.isEmpty()) {
            throw new InvalidRefreshTokenException("Token not found in Redis");
        }

        try {
            return objectMapper.readValue(jsonValue, StoredTokenDto.class);
        } catch (JsonProcessingException e) {
            throw new InvalidRefreshTokenException("Failed to deserialize token");
        }
    }

    public String reIssueAccessToken(String refreshToken) {
        if (!existByToken(refreshToken)) {
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }

        Long userId = getUserIdFromToken(refreshToken);
        StoredTokenDto storedToken = getStoredTokenFromRedis(userId);

        return pureJwtUtil.generateAccessToken(userId, storedToken.getEmail());
    }


    @Transactional
    public ReIssueRefreshTokenDto reIssueRefreshToken(String refreshToken) {
        StoredTokenDto storedToken = validateAndGetStoredToken(refreshToken);
        Long userId = getUserIdFromToken(refreshToken);

        String newAccessToken = pureJwtUtil.generateAccessToken(userId, storedToken.getEmail());
        String newRefreshToken = pureJwtUtil.generateRefreshToken(userId);

        removeAllRefreshToken(userId);
        LocalDateTime expirationAt = pureJwtUtil.getExpirationFromToken(newRefreshToken);
        saveRefreshTokenWithEmail(newRefreshToken, userId, storedToken.getEmail(), expirationAt);

        return new ReIssueRefreshTokenDto(newAccessToken, newRefreshToken);
    }

    private StoredTokenDto validateAndGetStoredToken(String refreshToken) {
        Long userId = getUserIdFromToken(refreshToken);
        StoredTokenDto storedToken = getStoredTokenFromRedis(userId);

        if (!refreshToken.equals(storedToken.getRefreshToken())) {
            throw new InvalidRefreshTokenException("Token mismatch");
        }

        return storedToken;
    }

    public void removeAllRefreshToken(Long userId) {
        String key = "refresh_token:" + userId;
        redis.deleteSingleData(key);
    }

    public int calculateRemainingSeconds(String refreshToken) {
        LocalDateTime expirationAt = pureJwtUtil.getExpirationFromToken(refreshToken);

        if (expirationAt == null) {
            throw new InvalidRefreshTokenException("Cannot extract expiration from token");
        }

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(now, expirationAt);
        return (int) duration.getSeconds();
    }
}
