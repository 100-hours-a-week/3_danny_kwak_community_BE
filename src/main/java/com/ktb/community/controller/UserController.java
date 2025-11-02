package com.ktb.community.controller;

import com.ktb.community.dto.request.ChangePasswordRequestDto;
import com.ktb.community.dto.request.EmailCheckRequestDto;
import com.ktb.community.dto.request.ModifyNicknameRequestDto;
import com.ktb.community.dto.request.PasswordCheckRequestDto;
import com.ktb.community.dto.response.ApiResponseDto;
import com.ktb.community.dto.response.AvailabilityResponseDto;
import com.ktb.community.dto.response.CrudUserResponseDto;
import com.ktb.community.dto.response.UserInfoResponseDto;
import com.ktb.community.service.UserService;
import com.ktb.community.util.JwtRequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/email")
    ResponseEntity<ApiResponseDto<AvailabilityResponseDto>> checkEmail(@RequestBody @Valid EmailCheckRequestDto emailCheckDto, BindingResult bindingResult) {
        // 검증에서 문제가 발생했다면
        if (bindingResult.hasErrors()) {
            String message = (bindingResult.getFieldError("email") != null) ? bindingResult.getFieldError("email").getDefaultMessage() : "Not a valid request";
            return ResponseEntity.badRequest().body(ApiResponseDto.error(message));
        }

        AvailabilityResponseDto availabilityResponseDto = this.userService.checkDuplicateEmail(emailCheckDto.getEmail());
        return ResponseEntity.ok().body(ApiResponseDto.success(availabilityResponseDto));
    }


    @PostMapping("/password")
    ResponseEntity<ApiResponseDto<AvailabilityResponseDto>> checkValidityPassword(@RequestBody @Valid PasswordCheckRequestDto passwordCheckRequestDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String message = (bindingResult.getFieldError("password") != null)
                    ? bindingResult.getFieldError("password").getDefaultMessage()
                    : "Not a valid request";
            return ResponseEntity.badRequest().body(ApiResponseDto.error(message));
        }

        AvailabilityResponseDto availabilityResponseDto = this.userService.checkValidityPassword(passwordCheckRequestDto.getPassword());
        return ResponseEntity.ok().body(ApiResponseDto.success(availabilityResponseDto));
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiResponseDto<?>> changePassword(
            @RequestBody @Valid ChangePasswordRequestDto changePasswordRequestDto,
            BindingResult bindingResult,
            HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "Not a valid request";
            return ResponseEntity.badRequest().body(ApiResponseDto.error(message));
        }

        CrudUserResponseDto crudUserResponseDto =
                this.userService.changePassword(JwtRequestUtil.getEmail(request), changePasswordRequestDto);

        return ResponseEntity.ok().body(ApiResponseDto.success(crudUserResponseDto));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<?>> getMyInformation(HttpServletRequest request) {
        UserInfoResponseDto userInfoResponseDto = this.userService.readMyInfo(JwtRequestUtil.getEmail(request));
        return ResponseEntity.ok().body(ApiResponseDto.success(userInfoResponseDto));
    }

    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponseDto<?>> patchNickname(
            @RequestBody @Valid ModifyNicknameRequestDto modifyNicknameRequestDto,
            HttpServletRequest request) {
        CrudUserResponseDto crudUserResponseDto =
                this.userService.changeNickname(JwtRequestUtil.getEmail(request), modifyNicknameRequestDto);

        return ResponseEntity.ok().body(ApiResponseDto.success(crudUserResponseDto));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponseDto<?>> deleteUser(HttpServletRequest request) {
        //TODO : 삭제 로직 구현하기
        this.userService.removeUser(JwtRequestUtil.getEmail(request));
        return ResponseEntity.ok().body(ApiResponseDto.success("test중"));
    }
}
