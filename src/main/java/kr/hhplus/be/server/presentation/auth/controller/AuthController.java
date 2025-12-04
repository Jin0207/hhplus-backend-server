package kr.hhplus.be.server.presentation.auth.controller;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.hhplus.be.server.application.user.service.UserService;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.presentation.auth.controller.dto.request.LoginRequest;
import kr.hhplus.be.server.presentation.auth.controller.dto.request.SignupRequest;
import kr.hhplus.be.server.presentation.auth.controller.dto.response.LoginResponse;
import kr.hhplus.be.server.presentation.user.dto.response.UserResponse;
import kr.hhplus.be.server.support.response.ApiResponse;
import kr.hhplus.be.server.support.security.provider.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ApiResponse<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = User.create(request.accountId(), encodedPassword);
        User savedUser = userService.createUser(user);
        
        return ApiResponse.success("회원가입 성공", UserResponse.from(savedUser));
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // 인증 처리
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.accountId(),
                request.password()
            )
        );

        // 사용자 조회
        User user = userService.getUserByAccountId(request.accountId());

        // JWT 토큰 생성
        String token = jwtTokenProvider.createToken(user.accountId(), user.id());

        return ApiResponse.success("로그인 성공", 
            new LoginResponse(token, UserResponse.from(user)));
    }

    /**
     * 토큰 검증 (테스트용)
     * GET /api/auth/validate
     */
    @GetMapping("/validate")
    public ApiResponse<Boolean> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ApiResponse.success("유효하지 않은 토큰", false);
        }

        String token = authHeader.substring(7);
        boolean isValid = jwtTokenProvider.validateToken(token);
        
        return ApiResponse.success("토큰 검증 완료", isValid);
    }
}