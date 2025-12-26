package kr.hhplus.be.server.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.enums.PaymentStatus;
import kr.hhplus.be.server.domain.payment.enums.PaymentType;
import kr.hhplus.be.server.support.exception.BusinessException;

class PaymentTest {

    @Test
    @DisplayName("결제 생성 시 초기 상태는 PENDING이며 외부동기화는 false이다")
    void createPayment_initial_state() {
        // when
        Payment payment = Payment.create(1L, 1L, "IDEM-KEY-001", 50000L, PaymentType.CARD);

        // then
        assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.externalSync()).isFalse();
    }

    @Test
    @DisplayName("결제 완료 시 상태가 COMPLETED로 변경되고 트랜잭션 ID가 기록된다")
    void completePayment_success() {
        // given
        Payment payment = Payment.create(1L, 1L, "IDEM-KEY-001", 50000L, PaymentType.CARD);
        String tid = "PG-TID-12345";

        // when
        Payment completed = payment.complete(tid);

        // then
        assertThat(completed.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(completed.successDttm()).isNotNull();
    }

    @Test
    @DisplayName("이미 처리된 결제를 다시 완료 처리하면 예외가 발생한다")
    void completePayment_already_processed() {
        // given
        Payment payment = Payment.create(1L, 1L, "IDEM-KEY-001", 50000L, PaymentType.CARD)
                            .complete("TID-1");

        // when & then
        assertThatThrownBy(() -> payment.complete("TID-2"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 실패 시 실패 사유가 기록되고 상태가 FAILED가 된다")
    void failPayment_success() {
        // given
        Payment payment = Payment.create(1L, 1L, "IDEM-KEY-001", 50000L, PaymentType.CARD);
        String reason = "잔액 부족";

        // when
        Payment failed = payment.fail(reason);

        // then
        assertThat(failed.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failed.failReason()).isEqualTo(reason);
    }
}