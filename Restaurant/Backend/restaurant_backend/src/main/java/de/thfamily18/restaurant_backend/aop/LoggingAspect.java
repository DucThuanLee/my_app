package de.thfamily18.restaurant_backend.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("execution(* de.thfamily18.restaurant_backend.controller..*(..))")
    public Object logTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        log.info("{} executed in {} ms", pjp.getSignature(), System.currentTimeMillis() - start);
        return result;
    }
}
