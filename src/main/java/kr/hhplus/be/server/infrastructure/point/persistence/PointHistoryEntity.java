package kr.hhplus.be.server.infrastructure.point.persistence;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.hhplus.be.server.domain.point.entity.PointHistory;
import kr.hhplus.be.server.domain.point.enums.PointType;
import kr.hhplus.be.server.infrastructure.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class PointHistoryEntity extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer point;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PointType type;

    @Column(length = 100)
    private String comment;

    /**
     * Domain -> Entity 변환
     */
    public static PointHistoryEntity from(PointHistory history) {
        return PointHistoryEntity.builder()
            .id(history.id())
            .userId(history.userId())
            .point(history.point())
            .type(history.type())
            .comment(history.comment())
            .build();
    }

    /**
     * Entity -> Domain 변환
     */
    public PointHistory toDomain() {
        return new PointHistory(
            this.id,
            this.userId,
            this.point,
            this.type,
            this.comment,
            getCrtDttm()
        );
    }
}
