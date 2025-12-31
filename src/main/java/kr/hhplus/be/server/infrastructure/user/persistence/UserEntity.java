package kr.hhplus.be.server.infrastructure.user.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.infrastructure.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class UserEntity extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;             // 식별자

    @Column(name = "account_id", nullable = false, unique = true, length = 50)
    private String accountId;       // 유저ID

    @Column(nullable = false, length = 255)
    private String password;        // PWD

    @Column(nullable = false)
    private Long point;          // 보유포인트

    /**
     * Domain -> Entity 변환
     */
    public static UserEntity from(User user) {
        return UserEntity.builder()
            .id(user.id())
            .accountId(user.accountId())
            .password(user.password())
            .point(user.point())
            .build();
    }

    /**
     * Entity -> Domain 변환
     */
    public User toDomain() {
        return new User(
            this.id,
            this.accountId,
            this.password,
            this.point,
            this.getCrtDttm(),
            this.getUpdDttm()
        );
    }

    /**
     * Domain 정보로 Entity 업데이트
     */
    public void updateFromDomain(User user) {
        this.point = user.point();
    }
}
