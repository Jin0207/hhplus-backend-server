package kr.hhplus.be.server.presentation.user.dto.response;

import kr.hhplus.be.server.domain.user.entity.User;
import lombok.Builder;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 정보를 담고 있는 클래스")
@Builder
public record UserResponse(
    @Schema(description = "사용자 ID")
    Long id,
    @Schema(description = "계정 ID")
    String accountId,
    @Schema(description = "보유포인트")
    Long point,
    @Schema(description = "생성일")
    LocalDateTime crtDttm,
    @Schema(description = "수정일")
    LocalDateTime updDttm
) {
    public static UserResponse from(User user) {
        return UserResponse.builder()
            .id(user.id())
            .accountId(user.accountId())
            .point(user.point())
            .crtDttm(user.crtDttm())
            .updDttm(user.updDttm())
            .build();
    }
}

