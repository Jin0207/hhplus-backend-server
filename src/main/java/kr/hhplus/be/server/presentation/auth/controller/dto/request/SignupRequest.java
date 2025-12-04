package kr.hhplus.be.server.presentation.auth.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank(message = "계정 ID는 필수입니다")
    @Size(max = 50, message = "계정 ID는 50자 이하여야 합니다")
    String accountId,
    
    @NotBlank(message = "비밀번호는 필수입니다")
    String password
) {

}
