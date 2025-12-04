package kr.hhplus.be.server.presentation.auth.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "계정 ID는 필수입니다")
    String accountId,
    
    @NotBlank(message = "비밀번호는 필수입니다")
    String password
) {

}
