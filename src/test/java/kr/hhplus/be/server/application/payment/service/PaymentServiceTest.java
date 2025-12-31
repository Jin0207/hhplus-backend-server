package kr.hhplus.be.server.application.payment.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class PaymentServiceTest {
    @InjectMocks private PaymentService paymentService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("실패: 이미 완료된 결제에 대해 멱등성 검사 시 예외가 발생한다")
    void checkForIdempotencyKey_AlreadyCompleted() {
        // given
        String key = "DUPLICATE-KEY";
        Payment existingPayment = new Payment(1L, 1L, 1L, key, 1000L,
            PaymentStatus.COMPLETED, PaymentType.CARD, null, "TID", null,
            LocalDateTime.now(), null, false, null, LocalDateTime.now(), null);

        // Redis Mock 설정: 락 획득 실패 (이미 존재)
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(false); // 락 획득 실패

        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingPayment));

        // when & then
        assertThatThrownBy(() -> paymentService.checkForIdempotencyKey(key))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_ALREADY_PROCESSED);
    }

}