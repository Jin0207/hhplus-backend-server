package kr.hhplus.be.server.infrastructure.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 분산 락을 적용하기 위한 어노테이션
 * Redis 기반으로 동시성 제어를 수행한다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WithDistributedLock {

    /**
     * 락 키 (SpEL 표현식 지원)
     * 예: "order:#{#userId}" 또는 "payment:#{#request.orderId}"
     */
    String key();

    /**
     * 락 대기 시간 (초)
     */
    long waitTime() default 5;

    /**
     * 락 유지 시간 (초)
     */
    long leaseTime() default 10;
}
