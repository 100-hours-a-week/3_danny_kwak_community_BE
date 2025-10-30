package com.ktb.community.service;

import com.ktb.community.dto.request.LoginRequestDto;
import com.ktb.community.dto.request.SignUpRequestDto;
import com.ktb.community.entity.User;
import com.ktb.community.exception.custom.DuplicateEmailException;
import com.ktb.community.exception.custom.InvalidCredentialsException;
import com.ktb.community.exception.custom.UserNotFoundException;
import com.ktb.community.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("session")
public class SessionAuthService {
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    public SessionAuthService(UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder, SessionService sessionService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
    }

    public Long signUpUser(SignUpRequestDto signUpRequestDto) {
        // 비밀번호와 비밀번호 확인이 동일한지 검사
        if (!signUpRequestDto.getPassword().equals(signUpRequestDto.getPasswordConfirm())) {
            throw new IllegalArgumentException("Password and Password Confirm is not same.");
        }

        // email이 중복되는지 확인
        if (this.userRepository.existsByEmail(signUpRequestDto.getEmail())) {
            throw new DuplicateEmailException("This email already exists");
        }

        if (this.userRepository.existsByNickname(signUpRequestDto.getNickname())) {
            throw new IllegalArgumentException("This nickname is already exist");
        }

        User user = new User();
        user.setEmail(signUpRequestDto.getEmail());
        if (!this.userService.checkValidityPassword(signUpRequestDto.getPassword()).getIsAvailable()) {
            throw new IllegalArgumentException("Password does not meet requirements");
        }
        // bcrypt로 암호화
        user.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));
        user.setNickname(signUpRequestDto.getNickname());
        user.setProfileImage(signUpRequestDto.getProfileImage());

        return this.userRepository.save(user).getId();
    }

    @Transactional
    public String login(LoginRequestDto loginRequestDto, HttpServletResponse response) {
        // 사용자 조회
        User user = this.userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // 비밀번호 검증
        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // 세션 생성 + 쿠키 설정
        String sessionId = sessionService.createSession(response, user.getId(), user.getNickname());

        return sessionId;
    }
}