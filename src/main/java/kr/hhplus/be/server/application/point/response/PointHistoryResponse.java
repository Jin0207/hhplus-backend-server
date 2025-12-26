package kr.hhplus.be.server.application.point.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.domain.point.entity.PointHistory;
import lombok.Builder;

@Schema(description = "포인트내역을 담고 있는 클래스")
@Builder
public record PointHistoryResponse(
    @Schema(description="포인트내역 ID")
    Long id,
    @Schema(description="사용자 ID")
    Long userId,
    @Schema(description="포인트")
    Long point,
    @Schema(description="포인트타입코드")
    String type,
    @Schema(description="포인트타입")
    String typeDescription,
    @Schema(description="비고")
    String comment,
    @Schema(description="생성일")
    LocalDateTime crtDttm
) {
    public static PointHistoryResponse from(PointHistory history) {
        return PointHistoryResponse.builder()
            .id(history.id())
            .userId(history.userId())
            .point(history.point())
            .type(history.type().name())
            .typeDescription(history.type().getDescription())
            .comment(history.comment())
            .crtDttm(history.crtDttm())
            .build();
    }
}
