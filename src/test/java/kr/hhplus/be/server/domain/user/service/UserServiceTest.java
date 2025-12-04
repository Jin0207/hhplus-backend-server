package kr.hhplus.be.server.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
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
import kr.hhplus.be.server.domain.user.repository.UserRepository;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 포인트 충전/사용 TDD")
public class UserServiceTest{
    @Mock
    private UserRepository userRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;
    
    @InjectMocks
    private UserService userService;

    private User mockUser;
    private List<PointHistory> mockHistory;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        mockUser = new User(1L, "testuser", "password", 10000, now, null);
        
        mockHistory = List.of(
                new PointHistory(1L, 1L, 5000, PointType.CHARGE, null, now),
                new PointHistory(2L, 1L, 1000, PointType.USE, null, now)
        );
    }

    @Test
    @DisplayName("포인트내역조회 - 성공")
    public void 포인트_내역_조회_성공() {
        // given
        when(pointHistoryRepository.findByUserId(1L))
            .thenReturn(mockHistory);
        // when
        List<PointHistory> histories = userService.getPointHistory(1L);

        // then
        assertNotNull(histories);
        assertEquals(2, histories.size());
        assertEquals(PointType.CHARGE, histories.get(0).type());
        assertEquals(PointType.USE, histories.get(1).type());

        // Repository 호출 검증
        verify(pointHistoryRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("포인트 내역 조회 - 빈 내역 조회")
    public void 포인트_내역_조회_빈내역() {
        // given: 사용자 존재, 내역 없음
        when(pointHistoryRepository.findByUserId(1L))
            .thenReturn(List.of());

        // when
        List<PointHistory> histories = userService.getPointHistory(1L);

        // then
        assertNotNull(histories);      
        assertTrue(histories.isEmpty()); 

        verify(pointHistoryRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("포인트 충전 - 성공")
    public void 포인트_충전_성공() {
        // given
        Integer chargeAmount = 5000;
        User chargedUser = new User(
            mockUser.id(),
            mockUser.accountId(),
            mockUser.password(),
            15000,  // 10000 + 5000
            mockUser.crtDttm(),
            LocalDateTime.now()
        );
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(chargedUser);
        when(pointHistoryRepository.save(any())).thenReturn(null);
        
        // when
        User result = userService.chargePoint(1L, chargeAmount, "");

        // then
        assertNotNull(result);
        assertEquals(15000, result.point());
        
        // 검증
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
        verify(pointHistoryRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("포인트 충전 - 0 이하 금액으로 실패")
    void 포인트_충전_실패_최소_금액() {
        // Given
        Integer invalidAmount = -1000;
        // When
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userService.chargePoint(1L, invalidAmount, "")
        );

        // Then
        assertEquals(ErrorCode.CHARGE_LESS_THAN_ZERO, exception.getErrorCode());
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("포인트 충전 - 최대보유포인트 금액이상으로 실패")
    void 포인트_충전_실패_최대_보유_포인트() {
        // Given
        Integer invalidAmount = 1_000_001;
        when(userRepository.findById(mockUser.id())).thenReturn(Optional.of(mockUser));
        
        // When
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userService.chargePoint(1L, invalidAmount, "")
        );

        // Then
        assertEquals(ErrorCode.POINT_BALANCE_MAX, exception.getErrorCode());
        verify(userRepository, times(1)).findById(mockUser.id());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("포인트 충전 - 존재하지 않는 사용자")
    void 포인트_충전_실패_없는_사용자() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userService.chargePoint(999L, 5000, "")
        );

        // Then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("포인트 사용 - 성공")
    void 포인트_사용() {
        // Given
        Integer useAmount = 3000;
        User usedUser = new User(
            mockUser.id(),
            mockUser.accountId(),
            mockUser.password(),
            7000,
            mockUser.crtDttm(),
            LocalDateTime.now()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(usedUser);
        when(pointHistoryRepository.save(any())).thenReturn(null);

        // When
        User result = userService.usePoint(1L, useAmount, "");

        // Then
        assertNotNull(result);
        assertEquals(7000, result.point());
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
        verify(pointHistoryRepository).save(any());
    }

    @Test
    @DisplayName("포인트 사용 - 잔액 부족으로 실패")
    void 포인트_사용_실패_잔액_부족() {
        // Given
        Integer useAmount = 20000;
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // When
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userService.usePoint(1L, useAmount, "")
        );

        // Then
        assertEquals(ErrorCode.POINT_BALANCE_INSUFFICIENT, exception.getErrorCode());
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("포인트 잔액 조회 - 성공")
    void 포인트_잔액_조회() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // When
        Integer balance = userService.getPointBalance(1L);

        // Then
        assertEquals(10000, balance);
        verify(userRepository).findById(1L);
    }
    
    @Test
    @DisplayName("포인트 충전 - 최소값 1 충전 성공")
    void 포인트_충전_최소_금액() {
        // Given
        Integer minAmount = 1;
        User chargedUser = new User(
            mockUser.id(),
            mockUser.accountId(),
            mockUser.password(),
            10001,
            mockUser.crtDttm(),
            LocalDateTime.now()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(chargedUser);
        when(pointHistoryRepository.save(any())).thenReturn(null);

        // When
        User result = userService.chargePoint(1L, minAmount, "");

        // Then
        assertEquals(10001, result.point());
    }

}
