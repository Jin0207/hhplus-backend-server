package kr.hhplus.be.server.infrastructure.lock;

import java.lang.reflect.Method;
import java.time.Duration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 분산 락 AOP Aspect
 * @WithDistributedLock 어노테이션이 적용된 메서드에 대해 Redis 기반 분산 락을 적용한다.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DistributedLockAspect {

    private final StringRedisTemplate redisTemplate;
    private final ExpressionParser parser = new SpelExpressionParser();
    private static final String LOCK_PREFIX = "lock:";

    @Around("@annotation(kr.hhplus.be.server.infrastructure.lock.WithDistributedLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        WithDistributedLock annotation = method.getAnnotation(WithDistributedLock.class);

        String lockKey = LOCK_PREFIX + parseKey(joinPoint, annotation.key());
        long waitTime = annotation.waitTime() * 1000L;
        long leaseTime = annotation.leaseTime();
        String lockValue = Thread.currentThread().getName() + ":" + System.currentTimeMillis();

        boolean acquired = tryAcquireLock(lockKey, lockValue, leaseTime, waitTime);

        if (!acquired) {
            log.warn("[DistributedLock] 락 획득 실패: key={}", lockKey);
            throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED);
        }

        try {
            log.debug("[DistributedLock] 락 획득 성공: key={}", lockKey);
            return joinPoint.proceed();
        } finally {
            releaseLock(lockKey, lockValue);
            log.debug("[DistributedLock] 락 해제: key={}", lockKey);
        }
    }

    /**
     * 락 획득 시도 (스핀락 방식)
     * waitTime이 0이면 즉시 한 번만 시도 후 반환
     */
    private boolean tryAcquireLock(String lockKey, String lockValue, long leaseTime, long waitTime) {
        long startTime = System.currentTimeMillis();
        long sleepTime = 50L; // 50ms 간격으로 재시도

        // do-while 구조로 최소 한 번은 시도
        do {
            Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(leaseTime));

            if (Boolean.TRUE.equals(success)) {
                return true;
            }

            // waitTime이 0이면 즉시 반환 (재시도 없음)
            if (waitTime == 0) {
                return false;
            }

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } while (System.currentTimeMillis() - startTime < waitTime);

        return false;
    }

    /**
     * 락 해제 (본인이 획득한 락만 해제)
     */
    private void releaseLock(String lockKey, String lockValue) {
        String currentValue = redisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(currentValue)) {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * SpEL 표현식을 파싱하여 실제 락 키를 생성한다.
     * 두 가지 형태의 표현식을 지원:
     * 1. #{...} 형태: "order:#{#userId}"
     * 2. 직접 SpEL 형태: "'payment:idempotency:' + #request.idempotencyKey()"
     */
    private String parseKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        // 1. #{...} 형태 처리
        if (keyExpression.contains("#{")) {
            String spel = keyExpression.replaceAll("#\\{([^}]+)}", "$1");
            Object value = parser.parseExpression(spel).getValue(context);
            return keyExpression.replaceAll("#\\{[^}]+}", String.valueOf(value));
        }

        // 2. 직접 SpEL 표현식 처리 (변수 참조 # 또는 문자열 연결 + 포함 시)
        if (keyExpression.contains("#") || keyExpression.contains("+")) {
            try {
                Object value = parser.parseExpression(keyExpression).getValue(context);
                return String.valueOf(value);
            } catch (Exception e) {
                log.warn("[DistributedLock] SpEL 파싱 실패, 원본 키 사용: key={}, error={}",
                    keyExpression, e.getMessage());
                return keyExpression;
            }
        }

        return keyExpression;
    }
}
