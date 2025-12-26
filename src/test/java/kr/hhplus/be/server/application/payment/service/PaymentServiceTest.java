package kr.hhplus.be.server.application.payment.service;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    @DisplayName("실패: 이미 완료된 결제에 대해 멱등성 검사 시 예외가 발생한다")
    void checkForIdempotencyKey_AlreadyCompleted() {
        // given
        String key = "DUPLICATE-KEY";
        Payment existingPayment = new Payment(1L, 1L, 1L, key, 1000L, 
            PaymentStatus.COMPLETED, PaymentType.CARD, null, "TID", null, 
            LocalDateTime.now(), null, false, null, LocalDateTime.now(), null);

        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingPayment));

        // when & then
        assertThatThrownBy(() -> paymentService.checkForIdempotencyKey(key))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_ALREADY_PROCESSED);
    }
}