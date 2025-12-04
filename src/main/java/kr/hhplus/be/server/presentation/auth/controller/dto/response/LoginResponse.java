package kr.hhplus.be.server.presentation.auth.controller.dto.response;

import kr.hhplus.be.server.presentation.user.dto.response.UserResponse;

public record LoginResponse(
    String token,
    UserResponse user
) {

}
