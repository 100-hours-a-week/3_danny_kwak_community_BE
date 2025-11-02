package com.ktb.community.purejwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Component
public class PureJwtAuthenticationFilter extends OncePerRequestFilter {

    private final PureJwtUtil pureJwtUtil;
    private final ObjectMapper objectMapper;

    private static final String[] EXCLUDED_PATHS = {
            "/auth",
            "/auth/login",
            "/auth/consent",
            "/users/email",
            "/users/password"
    };

    private static final String[] EXCLUDED_PATH_PATTERNS = {
            "GET:/posts$"  // GET /posts만 허용 (목록 조회)
    };

    @Autowired
    public PureJwtAuthenticationFilter(PureJwtUtil pureJwtUtil, ObjectMapper objectMapper) {
        this.pureJwtUtil = pureJwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // CORS preflight 요청(OPTIONS)은 필터링 제외
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        if (Arrays.stream(EXCLUDED_PATHS).anyMatch(path::startsWith)) {
            return true;
        }

        String methodPath = method + ":" + path;
        return Arrays.stream(EXCLUDED_PATH_PATTERNS)
                .anyMatch(pattern -> methodPath.matches(pattern.replace("$", "")));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Optional<String> token = extractToken(request);

        if (token.isEmpty()) {
            sendErrorResponse(response, 401, "Need authorization token");
            return;
        }

        if (!validateAndSetAttribute(token.get(), request)) {
            sendErrorResponse(response, 401, "Not valid token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        return extractTokenFromHeader(request).or(() -> extractTokenFromCookie(request));
    }

    private Optional<String> extractTokenFromHeader(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Authorization"))
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7));
    }

    private Optional<String> extractTokenFromCookie(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies())
                .stream()
                .flatMap(Arrays::stream)
                .filter(cookie -> "accessToken".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private boolean validateAndSetAttribute(String token, HttpServletRequest request) {
        try {
            var jws = pureJwtUtil.parse(token);
            if (jws == null) {
                return false;
            }
            Claims body = jws.getBody();
            request.setAttribute("userId", Long.valueOf(body.getSubject()));
            request.setAttribute("email", body.get("email", String.class));
            request.setAttribute("token", token);  // 검증된 토큰도 저장
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, String> errorResponse = Map.of("error", message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
