package com.ktb.community.service;

import com.ktb.community.session.SessionData;
import com.ktb.community.session.SessionUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    private final SessionUtil sessionUtil;

    public SessionService(SessionUtil sessionUtil) {
        this.sessionUtil = sessionUtil;
    }

    // Cookie에서 세션 ID 추출
    public String getSessionIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("SID".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    // 현재 요청의 세션 데이터 가져오기
    public SessionData getCurrentSession(HttpServletRequest request) {
        String sessionId = getSessionIdFromCookie(request);
        if (sessionId == null) {
            return null;
        }
        return sessionUtil.getSession(sessionId);
    }

    // 세션 생성 + 쿠키 설정
    public String createSession(HttpServletResponse response, Long userId, String nickname) {
        return   sessionUtil.createSession(userId, nickname);
    }

    // 세션 삭제 + 쿠키 삭제
    public boolean removeSession(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = getSessionIdFromCookie(request);
        if (sessionId == null) {
            return false;
        }

        // Redis에서 세션 삭제
        return sessionUtil.removeSession(sessionId);
    }
}
