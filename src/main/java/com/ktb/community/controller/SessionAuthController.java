package com.ktb.community.controller;

import com.ktb.community.dto.request.LoginRequestDto;
import com.ktb.community.dto.request.SignUpRequestDto;
import com.ktb.community.dto.response.ApiResponseDto;
import com.ktb.community.dto.response.CrudUserResponseDto;
import com.ktb.community.service.SessionAuthService;
import com.ktb.community.service.SessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Profile("session")
public class SessionAuthController {
    private final SessionAuthService sessionAuthService;
    private final SessionService sessionService;

    public SessionAuthController(SessionAuthService sessionAuthService, SessionService sessionService) {
        this.sessionAuthService = sessionAuthService;
        this.sessionService = sessionService;
    }

    @PostMapping()
    public ResponseEntity<ApiResponseDto<?>> signUp(@RequestBody @Valid SignUpRequestDto signUpRequestDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String message = "Not valid form";
            return ResponseEntity.badRequest().body(ApiResponseDto.error(message));
        }

        Long userId = this.sessionAuthService.signUpUser(signUpRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.success(new CrudUserResponseDto(userId)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<?>> login(
            @RequestBody @Valid LoginRequestDto loginRequestDto,
            HttpServletResponse response) {

        // 세션 생성 + 쿠키 설정
        String sessionId = sessionAuthService.login(loginRequestDto, response);
        Cookie cookie = new Cookie("SID", sessionId);
        cookie.setHttpOnly(true);  // XSS 방어
        cookie.setPath("/");
        cookie.setMaxAge(3600);  // 1시간
        response.addCookie(cookie);

        return ResponseEntity.ok(ApiResponseDto.success("로그인 성공"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<?>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        // 세션 삭제 + 쿠키 삭제
        boolean success = sessionService.removeSession(request, response);

        // 쿠키 삭제
        Cookie cookie = new Cookie("SID", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        if (success) {
            return ResponseEntity.ok(ApiResponseDto.success("로그아웃 성공"));
        } else {
            return ResponseEntity.ok(ApiResponseDto.success("이미 로그아웃 상태입니다."));
        }
    }
}