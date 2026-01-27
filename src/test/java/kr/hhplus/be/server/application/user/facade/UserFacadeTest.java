package kr.hhplus.be.server.application.user.facade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.application.point.response.PointHistoryResponse;
import kr.hhplus.be.server.application.point.service.PointService;
import kr.hhplus.be.server.domain.point.entity.PointHistory;
import kr.hhplus.be.server.domain.point.enums.PointType;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.presentation.user.dto.response.UserResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserFacade 사용자 복합 작업 TDD")
public class UserFacadeTest {

    @Mock
    private PointService pointService;

    @InjectMocks
    private UserFacade userFacade;

    private User user;
    private User chargedUser;
    private User usedUser;
    private PointHistory chargeHistory;
    private PointHistory useHistory;
    private List<PointHistory> pointHistoryList;

    @BeforeEach
    void setUp() {
        // Given: 기본 사용자
        user = new User(
            1L,
            "testuser@example.com",
            "password123",
            10000L,
            LocalDateTime.now(),
            null
        );

        // Given: 포인트 충전 후 사용자
        chargedUser = new User(
            1L,
            "testuser@example.com",
            "password123",
            15000L,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        // Given: 포인트 사용 후 사용자
        usedUser = new User(
            1L,
            "testuser@example.com",
            "password123",
            7000L,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        // Given: 포인트 충전 이력
        chargeHistory = new PointHistory(
            1L,
            1L,
            5000L,
            PointType.CHARGE,
            "포인트 충전",
            LocalDateTime.now()
        );

        // Given: 포인트 사용 이력
        useHistory = new PointHistory(
            2L,
            1L,
            3000L,
            PointType.USE,
            "주문 결제",
            LocalDateTime.now()
        );

        // Given: 포인트 이력 목록
        pointHistoryList = List.of(chargeHistory, useHistory);
    }

    // ==================== 포인트 충전 테스트 ====================

    @Test
    @DisplayName("성공: 포인트 충전")
    void chargePoint_성공() {
        // Given
        Long userId = 1L;
        Long amount = 5000L;
        String comment = "포인트 충전";

        when(pointService.chargePoint(userId, amount, comment)).thenReturn(chargedUser);

        // When
        UserResponse result = userFacade.chargePoint(userId, amount, comment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.accountId()).isEqualTo("testuser@example.com");
        assertThat(result.point()).isEqualTo(15000L);
        verify(pointService, times(1)).chargePoint(userId, amount, comment);
    }

    @Test
    @DisplayName("성공: 여러 번 포인트 충전")
    void chargePoint_성공_여러_번() {
        // Given
        Long userId = 1L;
        Long[] amounts = {1000L, 2000L, 5000L};
        String[] comments = {"충전1", "충전2", "충전3"};

        User user1 = new User(1L, "testuser@example.com", "password123", 11000L, LocalDateTime.now(), LocalDateTime.now());
        User user2 = new User(1L, "testuser@example.com", "password123", 13000L, LocalDateTime.now(), LocalDateTime.now());
        User user3 = new User(1L, "testuser@example.com", "password123", 18000L, LocalDateTime.now(), LocalDateTime.now());

        when(pointService.chargePoint(userId, amounts[0], comments[0])).thenReturn(user1);
        when(pointService.chargePoint(userId, amounts[1], comments[1])).thenReturn(user2);
        when(pointService.chargePoint(userId, amounts[2], comments[2])).thenReturn(user3);

        // When
        UserResponse result1 = userFacade.chargePoint(userId, amounts[0], comments[0]);
        UserResponse result2 = userFacade.chargePoint(userId, amounts[1], comments[1]);
        UserResponse result3 = userFacade.chargePoint(userId, amounts[2], comments[2]);

        // Then
        assertThat(result1.point()).isEqualTo(11000L);
        assertThat(result2.point()).isEqualTo(13000L);
        assertThat(result3.point()).isEqualTo(18000L);
        verify(pointService, times(3)).chargePoint(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("성공: 포인트 충전 시 comment 정확히 전달")
    void chargePoint_성공_comment_전달() {
        // Given
        Long userId = 1L;
        Long amount = 5000L;
        String comment = "프로모션 이벤트 포인트";

        when(pointService.chargePoint(userId, amount, comment)).thenReturn(chargedUser);

        // When
        UserResponse result = userFacade.chargePoint(userId, amount, comment);

        // Then
        verify(pointService, times(1)).chargePoint(eq(userId), eq(amount), eq(comment));
    }

    // ==================== 포인트 사용 테스트 ====================

    @Test
    @DisplayName("성공: 포인트 사용")
    void usePoint_성공() {
        // Given
        Long userId = 1L;
        Long amount = 3000L;

        when(pointService.usePoint(userId, amount, null)).thenReturn(usedUser);

        // When
        UserResponse result = userFacade.usePoint(userId, amount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.accountId()).isEqualTo("testuser@example.com");
        assertThat(result.point()).isEqualTo(7000L);
        verify(pointService, times(1)).usePoint(userId, amount, null);
    }

    @Test
    @DisplayName("성공: 포인트 사용 시 comment는 null로 전달")
    void usePoint_성공_comment_null() {
        // Given
        Long userId = 1L;
        Long amount = 3000L;

        when(pointService.usePoint(userId, amount, null)).thenReturn(usedUser);

        // When
        UserResponse result = userFacade.usePoint(userId, amount);

        // Then
        verify(pointService, times(1)).usePoint(eq(userId), eq(amount), eq(null));
    }

    @Test
    @DisplayName("성공: 여러 번 포인트 사용")
    void usePoint_성공_여러_번() {
        // Given
        Long userId = 1L;
        Long[] amounts = {1000L, 2000L, 3000L};

        User user1 = new User(1L, "testuser@example.com", "password123", 9000L, LocalDateTime.now(), LocalDateTime.now());
        User user2 = new User(1L, "testuser@example.com", "password123", 7000L, LocalDateTime.now(), LocalDateTime.now());
        User user3 = new User(1L, "testuser@example.com", "password123", 4000L, LocalDateTime.now(), LocalDateTime.now());

        when(pointService.usePoint(userId, amounts[0], null)).thenReturn(user1);
        when(pointService.usePoint(userId, amounts[1], null)).thenReturn(user2);
        when(pointService.usePoint(userId, amounts[2], null)).thenReturn(user3);

        // When
        UserResponse result1 = userFacade.usePoint(userId, amounts[0]);
        UserResponse result2 = userFacade.usePoint(userId, amounts[1]);
        UserResponse result3 = userFacade.usePoint(userId, amounts[2]);

        // Then
        assertThat(result1.point()).isEqualTo(9000L);
        assertThat(result2.point()).isEqualTo(7000L);
        assertThat(result3.point()).isEqualTo(4000L);
        verify(pointService, times(3)).usePoint(anyLong(), anyLong(), eq(null));
    }

    // ==================== 포인트 잔액 조회 테스트 ====================

    @Test
    @DisplayName("성공: 포인트 잔액 조회")
    void getPointBalance_성공() {
        // Given
        Long userId = 1L;
        Long expectedBalance = 10000L;

        when(pointService.getPointBalance(userId)).thenReturn(expectedBalance);

        // When
        Long result = userFacade.getPointBalance(userId);

        // Then
        assertThat(result).isEqualTo(expectedBalance);
        verify(pointService, times(1)).getPointBalance(userId);
    }

    @Test
    @DisplayName("성공: 다양한 포인트 잔액 조회")
    void getPointBalance_성공_다양한_잔액() {
        // Given
        Long userId = 1L;
        Long[] balances = {0L, 1000L, 10000L, 100000L};

        for (Long balance : balances) {
            when(pointService.getPointBalance(userId)).thenReturn(balance);

            // When
            Long result = userFacade.getPointBalance(userId);

            // Then
            assertThat(result).isEqualTo(balance);
        }

        verify(pointService, times(4)).getPointBalance(userId);
    }

    @Test
    @DisplayName("성공: 여러 사용자의 포인트 잔액 조회")
    void getPointBalance_성공_여러_사용자() {
        // Given
        Long[] userIds = {1L, 2L, 3L};
        Long[] balances = {10000L, 20000L, 30000L};

        when(pointService.getPointBalance(1L)).thenReturn(balances[0]);
        when(pointService.getPointBalance(2L)).thenReturn(balances[1]);
        when(pointService.getPointBalance(3L)).thenReturn(balances[2]);

        // When & Then
        for (int i = 0; i < userIds.length; i++) {
            Long result = userFacade.getPointBalance(userIds[i]);
            assertThat(result).isEqualTo(balances[i]);
        }

        verify(pointService, times(1)).getPointBalance(1L);
        verify(pointService, times(1)).getPointBalance(2L);
        verify(pointService, times(1)).getPointBalance(3L);
    }

    // ==================== 포인트 이력 조회 테스트 ====================

    @Test
    @DisplayName("성공: 포인트 이력 조회")
    void getPointHistory_성공() {
        // Given
        Long userId = 1L;

        when(pointService.getPointHistory(userId)).thenReturn(pointHistoryList);

        // When
        List<PointHistoryResponse> result = userFacade.getPointHistory(userId);

        // Then
        assertThat(result).hasSize(2);

        // 첫 번째 이력 검증 (충전)
        PointHistoryResponse first = result.get(0);
        assertThat(first.id()).isEqualTo(1L);
        assertThat(first.userId()).isEqualTo(1L);
        assertThat(first.point()).isEqualTo(5000L);
        assertThat(first.type()).isEqualTo("CHARGE");
        assertThat(first.typeDescription()).isEqualTo("충전");
        assertThat(first.comment()).isEqualTo("포인트 충전");

        // 두 번째 이력 검증 (사용)
        PointHistoryResponse second = result.get(1);
        assertThat(second.id()).isEqualTo(2L);
        assertThat(second.userId()).isEqualTo(1L);
        assertThat(second.point()).isEqualTo(3000L);
        assertThat(second.type()).isEqualTo("USE");
        assertThat(second.typeDescription()).isEqualTo("사용");
        assertThat(second.comment()).isEqualTo("주문 결제");

        verify(pointService, times(1)).getPointHistory(userId);
    }

    @Test
    @DisplayName("성공: 포인트 이력 조회 - 단건")
    void getPointHistory_성공_단건() {
        // Given
        Long userId = 1L;
        List<PointHistory> singleHistory = List.of(chargeHistory);

        when(pointService.getPointHistory(userId)).thenReturn(singleHistory);

        // When
        List<PointHistoryResponse> result = userFacade.getPointHistory(userId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).point()).isEqualTo(5000L);
        assertThat(result.get(0).type()).isEqualTo("CHARGE");
        verify(pointService, times(1)).getPointHistory(userId);
    }

    @Test
    @DisplayName("성공: 포인트 이력 조회 - 빈 결과")
    void getPointHistory_성공_빈_결과() {
        // Given
        Long userId = 999L;
        List<PointHistory> emptyHistory = List.of();

        when(pointService.getPointHistory(userId)).thenReturn(emptyHistory);

        // When
        List<PointHistoryResponse> result = userFacade.getPointHistory(userId);

        // Then
        assertThat(result).isEmpty();
        verify(pointService, times(1)).getPointHistory(userId);
    }

    @Test
    @DisplayName("성공: 포인트 이력 조회 - 대량 데이터")
    void getPointHistory_성공_대량_데이터() {
        // Given
        Long userId = 1L;
        List<PointHistory> largeHistory = List.of(
            new PointHistory(1L, userId, 1000L, PointType.CHARGE, "충전1", LocalDateTime.now()),
            new PointHistory(2L, userId, 2000L, PointType.USE, "사용1", LocalDateTime.now()),
            new PointHistory(3L, userId, 3000L, PointType.CHARGE, "충전2", LocalDateTime.now()),
            new PointHistory(4L, userId, 4000L, PointType.USE, "사용2", LocalDateTime.now()),
            new PointHistory(5L, userId, 5000L, PointType.CHARGE, "충전3", LocalDateTime.now()),
            new PointHistory(6L, userId, 6000L, PointType.USE, "사용3", LocalDateTime.now()),
            new PointHistory(7L, userId, 7000L, PointType.CHARGE, "충전4", LocalDateTime.now()),
            new PointHistory(8L, userId, 8000L, PointType.USE, "사용4", LocalDateTime.now()),
            new PointHistory(9L, userId, 9000L, PointType.CHARGE, "충전5", LocalDateTime.now()),
            new PointHistory(10L, userId, 10000L, PointType.USE, "사용5", LocalDateTime.now())
        );

        when(pointService.getPointHistory(userId)).thenReturn(largeHistory);

        // When
        List<PointHistoryResponse> result = userFacade.getPointHistory(userId);

        // Then
        assertThat(result).hasSize(10);
        assertThat(result).allMatch(history -> history.userId().equals(userId));
        verify(pointService, times(1)).getPointHistory(userId);
    }

    @Test
    @DisplayName("성공: 포인트 이력 조회 결과의 데이터 변환 검증")
    void getPointHistory_성공_데이터_변환() {
        // Given
        Long userId = 1L;
        when(pointService.getPointHistory(userId)).thenReturn(pointHistoryList);

        // When
        List<PointHistoryResponse> result = userFacade.getPointHistory(userId);

        // Then
        // PointHistory -> PointHistoryResponse 변환 검증
        assertThat(result.get(0)).isNotNull();
        assertThat(result.get(0).id()).isEqualTo(chargeHistory.id());
        assertThat(result.get(0).userId()).isEqualTo(chargeHistory.userId());
        assertThat(result.get(0).point()).isEqualTo(chargeHistory.point());
        assertThat(result.get(0).type()).isEqualTo(chargeHistory.type().name());
        assertThat(result.get(0).typeDescription()).isEqualTo(chargeHistory.type().getDescription());
        assertThat(result.get(0).comment()).isEqualTo(chargeHistory.comment());
        assertThat(result.get(0).crtDttm()).isEqualTo(chargeHistory.crtDttm());
    }

    // ==================== UserResponse 변환 검증 테스트 ====================

    @Test
    @DisplayName("성공: chargePoint의 UserResponse 변환 검증")
    void chargePoint_성공_UserResponse_변환() {
        // Given
        Long userId = 1L;
        Long amount = 5000L;
        String comment = "포인트 충전";

        when(pointService.chargePoint(userId, amount, comment)).thenReturn(chargedUser);

        // When
        UserResponse result = userFacade.chargePoint(userId, amount, comment);

        // Then
        assertThat(result.id()).isEqualTo(chargedUser.id());
        assertThat(result.accountId()).isEqualTo(chargedUser.accountId());
        assertThat(result.point()).isEqualTo(chargedUser.point());
        assertThat(result.crtDttm()).isEqualTo(chargedUser.crtDttm());
        assertThat(result.updDttm()).isEqualTo(chargedUser.updDttm());
    }

    @Test
    @DisplayName("성공: usePoint의 UserResponse 변환 검증")
    void usePoint_성공_UserResponse_변환() {
        // Given
        Long userId = 1L;
        Long amount = 3000L;

        when(pointService.usePoint(userId, amount, null)).thenReturn(usedUser);

        // When
        UserResponse result = userFacade.usePoint(userId, amount);

        // Then
        assertThat(result.id()).isEqualTo(usedUser.id());
        assertThat(result.accountId()).isEqualTo(usedUser.accountId());
        assertThat(result.point()).isEqualTo(usedUser.point());
        assertThat(result.crtDttm()).isEqualTo(usedUser.crtDttm());
        assertThat(result.updDttm()).isEqualTo(usedUser.updDttm());
    }

    // ==================== 통합 시나리오 테스트 ====================

    @Test
    @DisplayName("성공: 포인트 충전 → 사용 → 잔액 조회 → 이력 조회 시나리오")
    void 포인트_전체_시나리오() {
        // Given
        Long userId = 1L;

        // 충전
        when(pointService.chargePoint(userId, 10000L, "초기 충전")).thenReturn(
            new User(1L, "testuser@example.com", "password123", 20000L, LocalDateTime.now(), LocalDateTime.now())
        );

        // 사용
        when(pointService.usePoint(userId, 5000L, null)).thenReturn(
            new User(1L, "testuser@example.com", "password123", 15000L, LocalDateTime.now(), LocalDateTime.now())
        );

        // 잔액 조회
        when(pointService.getPointBalance(userId)).thenReturn(15000L);

        // 이력 조회
        when(pointService.getPointHistory(userId)).thenReturn(pointHistoryList);

        // When: 충전
        UserResponse chargeResult = userFacade.chargePoint(userId, 10000L, "초기 충전");

        // Then: 충전 검증
        assertThat(chargeResult.point()).isEqualTo(20000L);

        // When: 사용
        UserResponse useResult = userFacade.usePoint(userId, 5000L);

        // Then: 사용 검증
        assertThat(useResult.point()).isEqualTo(15000L);

        // When: 잔액 조회
        Long balance = userFacade.getPointBalance(userId);

        // Then: 잔액 검증
        assertThat(balance).isEqualTo(15000L);

        // When: 이력 조회
        List<PointHistoryResponse> history = userFacade.getPointHistory(userId);

        // Then: 이력 검증
        assertThat(history).hasSize(2);

        // Then: 모든 서비스 호출 확인
        verify(pointService, times(1)).chargePoint(userId, 10000L, "초기 충전");
        verify(pointService, times(1)).usePoint(userId, 5000L, null);
        verify(pointService, times(1)).getPointBalance(userId);
        verify(pointService, times(1)).getPointHistory(userId);
    }
}
