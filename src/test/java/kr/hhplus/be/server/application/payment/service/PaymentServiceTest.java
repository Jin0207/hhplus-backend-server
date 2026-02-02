package kr.hhplus.be.server.application.payment.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.enums.PaymentStatus;
import kr.hhplus.be.server.domain.payment.enums.PaymentType;
import kr.hhplus.be.server.domain.payment.repository.PaymentRepository;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 결제 서비스 TDD")
class PaymentServiceTest {
    @InjectMocks private PaymentService paymentService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private Payment pendingPayment;
    private Payment completedPayment;
    private Payment failedPayment;

    @BeforeEach
    void setUp() {
        // Given: PENDING 상태 결제
        pendingPayment = new Payment(
            1L, 100L, 200L, "idempotency-key-123", 50000L,
            PaymentStatus.PENDING, PaymentType.POINT, null, null, null,
            LocalDateTime.now(), null, false, null, LocalDateTime.now(), null
        );

        // Given: COMPLETED 상태 결제
        completedPayment = new Payment(
            1L, 100L, 200L, "idempotency-key-123", 50000L,
            PaymentStatus.COMPLETED, PaymentType.POINT, "payment-gateway", "tx-12345", null,
            LocalDateTime.now(), LocalDateTime.now(), false, null, LocalDateTime.now(), LocalDateTime.now()
        );

        // Given: FAILED 상태 결제
        failedPayment = new Payment(
            1L, 100L, 200L, "idempotency-key-123", 50000L,
            PaymentStatus.FAILED, PaymentType.POINT, null, null, "잔액 부족",
            LocalDateTime.now(), null, false, null, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    // ==================== 멱등성 검사 테스트 ====================

    @Test
    @DisplayName("성공: 새로운 멱등성 키로 검사 - Redis 락 획득 성공, DB에 없음")
    void checkForIdempotencyKey_성공_새로운_키() {
        // Given
        String key = "NEW-KEY";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(true); // 락 획득 성공
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());

        // When & Then: 예외 발생하지 않음
        paymentService.checkForIdempotencyKey(key);

        // Then: Redis 락 획득 시도 확인
        verify(valueOperations, times(1)).setIfAbsent(
            "payment:idempotency:" + key,
            "locked",
            Duration.ofSeconds(30)
        );
    }

    @Test
    @DisplayName("실패: 이미 완료된 결제에 대해 멱등성 검사 시 PAYMENT_ALREADY_PROCESSED 예외")
    void checkForIdempotencyKey_실패_이미_완료된_결제() {
        // Given
        String key = "DUPLICATE-KEY";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(false); // 락 획득 실패
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(completedPayment));

        // When & Then
        assertThatThrownBy(() -> paymentService.checkForIdempotencyKey(key))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_ALREADY_PROCESSED);
    }

    @Test
    @DisplayName("실패: PENDING 상태 결제 중복 요청 시 DUPLICATE_PAYMENT_REQUEST 예외")
    void checkForIdempotencyKey_실패_PENDING_중복_요청() {
        // Given
        String key = "PENDING-KEY";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(false); // 락 획득 실패
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(pendingPayment));

        // When & Then
        assertThatThrownBy(() -> paymentService.checkForIdempotencyKey(key))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PAYMENT_REQUEST);
    }

    @Test
    @DisplayName("실패: FAILED 상태 결제 재시도 시 DUPLICATE_PAYMENT_REQUEST 예외")
    void checkForIdempotencyKey_실패_FAILED_재시도() {
        // Given
        String key = "FAILED-KEY";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(false); // 락 획득 실패
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(failedPayment));

        // When & Then
        assertThatThrownBy(() -> paymentService.checkForIdempotencyKey(key))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PAYMENT_REQUEST);
    }

    @Test
    @DisplayName("실패: Redis 락 획득 성공했으나 DB에 COMPLETED 결제 존재 시 예외")
    void checkForIdempotencyKey_실패_락_성공_DB에_완료_결제_존재() {
        // Given
        String key = "COMPLETED-IN-DB-KEY";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(true); // 락 획득 성공
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(completedPayment));

        // When & Then
        assertThatThrownBy(() -> paymentService.checkForIdempotencyKey(key))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_ALREADY_PROCESSED);
    }

    @Test
    @DisplayName("실패: Redis 락 획득 성공했으나 DB에 PENDING 결제 존재 시 예외")
    void checkForIdempotencyKey_실패_락_성공_DB에_PENDING_결제_존재() {
        // Given
        String key = "PENDING-IN-DB-KEY";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(true); // 락 획득 성공
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(pendingPayment));

        // When & Then
        assertThatThrownBy(() -> paymentService.checkForIdempotencyKey(key))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PAYMENT_REQUEST);
    }

    @Test
    @DisplayName("실패: Redis 락 획득 실패했으나 DB에 결제 없을 시 DUPLICATE_PAYMENT_REQUEST 예외")
    void checkForIdempotencyKey_실패_락_실패_DB에_결제_없음() {
        // Given
        String key = "NO-PAYMENT-IN-DB-KEY";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(false); // 락 획득 실패
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.checkForIdempotencyKey(key))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PAYMENT_REQUEST);
    }

    // ==================== DB 멱등성 검사 테스트 (AOP 분산락용) ====================

    @Test
    @DisplayName("성공: checkIdempotencyKeyInDb - DB에 결제 없음")
    void checkIdempotencyKeyInDb_성공_DB에_결제_없음() {
        // Given
        String key = "NEW-KEY-FOR-DB-CHECK";
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());

        // When & Then: 예외 발생하지 않음
        paymentService.checkIdempotencyKeyInDb(key);

        // Then: DB 조회 확인
        verify(paymentRepository, times(1)).findByIdempotencyKey(key);
    }

    @Test
    @DisplayName("실패: checkIdempotencyKeyInDb - DB에 COMPLETED 결제 존재")
    void checkIdempotencyKeyInDb_실패_COMPLETED_결제_존재() {
        // Given
        String key = "COMPLETED-KEY";
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(completedPayment));

        // When & Then
        assertThatThrownBy(() -> paymentService.checkIdempotencyKeyInDb(key))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_ALREADY_PROCESSED);
    }

    @Test
    @DisplayName("실패: checkIdempotencyKeyInDb - DB에 PENDING 결제 존재")
    void checkIdempotencyKeyInDb_실패_PENDING_결제_존재() {
        // Given
        String key = "PENDING-KEY";
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(pendingPayment));

        // When & Then
        assertThatThrownBy(() -> paymentService.checkIdempotencyKeyInDb(key))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PAYMENT_REQUEST);
    }

    @Test
    @DisplayName("실패: checkIdempotencyKeyInDb - DB에 FAILED 결제 존재")
    void checkIdempotencyKeyInDb_실패_FAILED_결제_존재() {
        // Given
        String key = "FAILED-KEY";
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(failedPayment));

        // When & Then
        assertThatThrownBy(() -> paymentService.checkIdempotencyKeyInDb(key))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PAYMENT_REQUEST);
    }

    // ==================== 결제 생성 테스트 ====================

    @Test
    @DisplayName("성공: 포인트 결제 생성")
    void createPayment_성공_포인트_결제() {
        // Given
        Long orderId = 100L;
        Long userId = 200L;
        String idempotencyKey = "create-key-123";
        Long price = 50000L;
        String paymentTypeString = "POINT";

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            return new Payment(1L, payment.orderId(), payment.userId(), payment.idempotencyKey(),
                payment.price(), payment.status(), payment.paymentType(), payment.paymentGateway(),
                payment.transactionId(), payment.failReason(), payment.requestDttm(), payment.successDttm(),
                payment.externalSync(), payment.syncedDttm(), payment.crtDttm(), payment.updDttm());
        });

        // When
        Payment result = paymentService.createPayment(orderId, userId, idempotencyKey, price, paymentTypeString);

        // Then
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.orderId()).isEqualTo(orderId);
        assertThat(savedPayment.userId()).isEqualTo(userId);
        assertThat(savedPayment.idempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(savedPayment.price()).isEqualTo(price);
        assertThat(savedPayment.paymentType()).isEqualTo(PaymentType.POINT);
        assertThat(savedPayment.status()).isEqualTo(PaymentStatus.PENDING);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("성공: 카드 결제 생성")
    void createPayment_성공_카드_결제() {
        // Given
        String paymentTypeString = "CARD";
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            return new Payment(1L, payment.orderId(), payment.userId(), payment.idempotencyKey(),
                payment.price(), payment.status(), payment.paymentType(), payment.paymentGateway(),
                payment.transactionId(), payment.failReason(), payment.requestDttm(), payment.successDttm(),
                payment.externalSync(), payment.syncedDttm(), payment.crtDttm(), payment.updDttm());
        });

        // When
        Payment result = paymentService.createPayment(100L, 200L, "card-key", 50000L, paymentTypeString);

        // Then
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.paymentType()).isEqualTo(PaymentType.CARD);
        assertThat(savedPayment.status()).isEqualTo(PaymentStatus.PENDING);
    }

    // ==================== 결제 조회 테스트 ====================

    @Test
    @DisplayName("성공: 결제 조회")
    void getPayment_성공() {
        // Given
        Long paymentId = 1L;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(pendingPayment));

        // When
        Payment result = paymentService.getPayment(paymentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository, times(1)).findById(paymentId);
    }

    @Test
    @DisplayName("실패: 존재하지 않는 결제 조회 시 PAYMENT_NOT_FOUND 예외")
    void getPayment_실패_결제_없음() {
        // Given
        Long paymentId = 999L;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.getPayment(paymentId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
    }

    // ==================== 결제 완료 테스트 ====================

    @Test
    @DisplayName("성공: 결제 완료 처리")
    void completePayment_성공() {
        // Given
        Long paymentId = 1L;
        String transactionId = "tx-12345";

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            return payment;
        });

        // When
        Payment result = paymentService.completePayment(paymentId, transactionId);

        // Then
        assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.transactionId()).isEqualTo(transactionId);
        assertThat(result.successDttm()).isNotNull();
        verify(paymentRepository, times(1)).findById(paymentId);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("실패: 존재하지 않는 결제 완료 시도 시 PAYMENT_NOT_FOUND 예외")
    void completePayment_실패_결제_없음() {
        // Given
        Long paymentId = 999L;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.completePayment(paymentId, "tx-12345"))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("실패: 이미 완료된 결제 재완료 시도 시 PAYMENT_ALREADY_PROCESSED 예외")
    void completePayment_실패_이미_완료됨() {
        // Given
        Long paymentId = 1L;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(completedPayment));

        // When & Then
        assertThatThrownBy(() -> paymentService.completePayment(paymentId, "tx-67890"))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_ALREADY_PROCESSED);
    }

    // ==================== 결제 실패 테스트 ====================

    @Test
    @DisplayName("성공: 결제 실패 처리")
    void failPayment_성공() {
        // Given
        Long paymentId = 1L;
        String failReason = "잔액 부족";

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            return payment;
        });

        // When
        Payment result = paymentService.failPayment(paymentId, failReason);

        // Then
        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.failReason()).isEqualTo(failReason);
        assertThat(result.successDttm()).isNull();
        verify(paymentRepository, times(1)).findById(paymentId);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("실패: 존재하지 않는 결제 실패 처리 시 PAYMENT_NOT_FOUND 예외")
    void failPayment_실패_결제_없음() {
        // Given
        Long paymentId = 999L;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.failPayment(paymentId, "잔액 부족"))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("성공: 다양한 실패 사유로 결제 실패 처리")
    void failPayment_성공_다양한_실패_사유() {
        // Given
        Long paymentId = 1L;
        String[] failReasons = {"잔액 부족", "네트워크 오류", "PG사 응답 없음", "카드 한도 초과"};

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        for (String failReason : failReasons) {
            Payment result = paymentService.failPayment(paymentId, failReason);
            assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
            assertThat(result.failReason()).isEqualTo(failReason);
        }
    }

}