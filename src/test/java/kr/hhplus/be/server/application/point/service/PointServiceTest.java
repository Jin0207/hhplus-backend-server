package kr.hhplus.be.server.application.point.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.application.user.service.UserService;
import kr.hhplus.be.server.domain.point.entity.PointHistory;
import kr.hhplus.be.server.domain.point.enums.PointType;
import kr.hhplus.be.server.domain.point.repository.PointHistoryRepository;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointService 테스트")
class PointServiceTest {
    
    @Mock
    private UserService userService;
    
    @Mock
    private PointHistoryRepository pointHistoryRepository;
    
    @InjectMocks
    private PointService pointService;
    
    @Test
    @DisplayName("성공: 포인트 충전이 성공한다")
    void 포인트_충전_성공() {
        // given
        Long userId = 1L;
        Long amount = 10000L;
        String comment = "충전";
        
        User user = new User(
            1L, "test@test.com", "테스터", 5000L, 
            LocalDateTime.now(), null
        );
        
        User chargedUser = new User(
            1L, "test@test.com", "테스터", 15000L,
            LocalDateTime.now(), LocalDateTime.now()
        );
        
        when(userService.getUser(userId)).thenReturn(user);
        when(userService.save(any(User.class))).thenReturn(chargedUser);
        when(pointHistoryRepository.save(any(PointHistory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // when
        User result = pointService.chargePoint(userId, amount, comment);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.point()).isEqualTo(15000L);
        
        verify(userService).getUser(userId);
        verify(userService).save(any(User.class));
        verify(pointHistoryRepository).save(argThat(history ->
            history.userId().equals(userId) &&
            history.point().equals(amount) &&
            history.type() == PointType.CHARGE &&
            history.comment().equals(comment)
        ));
    }
    
    @Test
    @DisplayName("실패: 포인트 충전 시 금액이 0 이하면 예외가 발생한다")
    void 포인트_충전_금액_0이하_예외() {
        // given
        Long userId = 1L;
        Long amount = -1000L;
        
        User user = new User(
            1L, "test@test.com", "테스터", 5000L,
            LocalDateTime.now(), null
        );
        
        when(userService.getUser(userId)).thenReturn(user);
        
        // when & then
        assertThatThrownBy(() -> pointService.chargePoint(userId, amount, "충전"))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHARGE_LESS_THAN_ZERO);
        
        verify(userService, never()).save(any());
        verify(pointHistoryRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("성공: 포인트 사용이 성공한다")
    void 포인트_사용_성공() {
        // given
        Long userId = 1L;
        Long amount = 3000L;
        String comment = "주문 결제";
        
        User user = new User(
            1L, "test@test.com", "테스터", 5000L,
            LocalDateTime.now(), null
        );
        
        User usedUser = new User(
            1L, "test@test.com", "테스터", 2000L,
            LocalDateTime.now(), LocalDateTime.now()
        );
        
        when(userService.getUser(userId)).thenReturn(user);
        when(userService.save(any(User.class))).thenReturn(usedUser);
        when(pointHistoryRepository.save(any(PointHistory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // when
        User result = pointService.usePoint(userId, amount, comment);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.point()).isEqualTo(2000L);
        
        verify(pointHistoryRepository).save(argThat(history ->
            history.type() == PointType.USE &&
            history.point().equals(amount)
        ));
    }
    
    @Test
    @DisplayName("실패: 포인트 사용 시 잔액이 부족하면 예외가 발생한다")
    void 포인트_사용_잔액_부족_예외() {
        // given
        Long userId = 1L;
        Long amount = 10000L;
        
        User user = new User(
            1L, "test@test.com", "테스터", 5000L,
            LocalDateTime.now(), null
        );
        
        when(userService.getUser(userId)).thenReturn(user);
        
        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, amount, "결제"))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_BALANCE_INSUFFICIENT);
        
        verify(userService, never()).save(any());
        verify(pointHistoryRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("성공: 포인트 환불이 성공한다")
    void 포인트_환불_성공() {
        // given
        Long userId = 1L;
        Long amount = 5000L;
        String reason = "환불";
        
        User user = new User(
            1L, "test@test.com", "테스터", 10000L,
            LocalDateTime.now(), null
        );
        
        User refundedUser = new User(
            1L, "test@test.com", "테스터", 15000L,
            LocalDateTime.now(), LocalDateTime.now()
        );
        
        when(userService.getUser(userId)).thenReturn(user);
        when(userService.save(any(User.class))).thenReturn(refundedUser);
        when(pointHistoryRepository.save(any(PointHistory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // when
        User result = pointService.refundPoint(userId, amount);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.point()).isEqualTo(15000L);
        
        verify(pointHistoryRepository).save(argThat(history ->
            history.type() == PointType.CHARGE &&
            history.point().equals(amount) &&
            history.comment().equals(reason)
        ));
    }
    
    @Test
    @DisplayName("성공: 포인트 이력 조회가 성공한다")
    void 포인트_이력_조회_성공() {
        // given
        Long userId = 1L;
        
        User user = new User(
            1L, "test@test.com", "테스터", 5000L,
            LocalDateTime.now(), null
        );
        
        List<PointHistory> histories = List.of(
            new PointHistory(
                1L, userId, 10000L, PointType.CHARGE, "충전",
                LocalDateTime.now()
            ),
            new PointHistory(
                2L, userId, 5000L, PointType.USE, "사용",
                LocalDateTime.now()
            )
        );
        
        when(userService.getUser(userId)).thenReturn(user);
        when(pointHistoryRepository.findByUserId(userId)).thenReturn(histories);
        
        // when
        List<PointHistory> result = pointService.getPointHistory(userId);
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).type()).isEqualTo(PointType.CHARGE);
        assertThat(result.get(1).type()).isEqualTo(PointType.USE);
    }
    
    @Test
    @DisplayName("실패: 존재하지 않는 사용자의 포인트 조회 시 예외가 발생한다")
    void 존재하지_않는_사용자_예외() {
        // given
        Long userId = 999L;
        
        when(userService.getUser(userId))
            .thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // when & then
        assertThatThrownBy(() -> pointService.getPointBalance(userId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }
    
    @Test
    @DisplayName("성공: 포인트 잔액 조회가 성공한다")
    void 포인트_잔액_조회_성공() {
        // given
        Long userId = 1L;
        
        User user = new User(
            1L, "test@test.com", "테스터", 12500L,
            LocalDateTime.now(), null
        );
        
        when(userService.getUser(userId)).thenReturn(user);
        
        // when
        Long balance = pointService.getPointBalance(userId);
        
        // then
        assertThat(balance).isEqualTo(12500L);
    }
}