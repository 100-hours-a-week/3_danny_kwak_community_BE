package com.ktb.community.userTest;

import com.ktb.community.dto.request.SignUpRequestDto;
import com.ktb.community.dto.response.AvailabilityResponseDto;
import com.ktb.community.entity.User;
import com.ktb.community.repository.UserRepository;
import com.ktb.community.service.AuthService;
import com.ktb.community.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@DisplayName("유저 서비스 테스트")
public class UserServiceTest {
    private UserRepository userRepository = Mockito.mock(UserRepository.class);
    private UserService userService = Mockito.mock(UserService.class);
    private PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
    private AuthService authService = new AuthService(userRepository, userService, passwordEncoder, null, null);


    @Test
    @DisplayName("회원가입 테스트")
    void createUserTest() {
        // given
        SignUpRequestDto signUpRequestDto = SignUpRequestDto
                .builder()
                .email("test@test.com")
                .password("password123!")
                .passwordConfirm("password123!")
                .nickname("danny")
                .build();

        User savedUser = new User();
        savedUser.setId(1L);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByNickname(anyString())).thenReturn(false);
        when(userService.checkValidityPassword(anyString()))
                .thenReturn(new AvailabilityResponseDto(true));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // when
        Long userId = authService.signUpUser(signUpRequestDto);

        // then
        assertThat(userId).isEqualTo(1L);
        Mockito.verify(userRepository).save(any(User.class));

    }
}
