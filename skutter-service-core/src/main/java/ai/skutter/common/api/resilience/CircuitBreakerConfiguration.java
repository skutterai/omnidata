package ai.skutter.common.api.resilience;

import ai.skutter.common.api.properties.SkutterApiProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for circuit breaker and retry patterns
 */
@Configuration
public class CircuitBreakerConfiguration {

    /**
     * Configure circuit breaker registry
     */
    @Bean
    @ConditionalOnProperty(prefix = "skutter.api.resilience", name = "circuit-breaker-enabled", havingValue = "true", matchIfMissing = true)
    public CircuitBreakerRegistry circuitBreakerRegistry(SkutterApiProperties apiProperties) {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slowCallRateThreshold(50)
            .slowCallDurationThreshold(Duration.ofSeconds(1))
            .permittedNumberOfCallsInHalfOpenState(10)
            .slidingWindowSize(100)
            .minimumNumberOfCalls(10)
            .waitDurationInOpenState(apiProperties.getResilience().getWaitDurationInOpenState())
            .build();
        
        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    /**
     * Configure default circuit breaker
     */
    @Bean
    @ConditionalOnProperty(prefix = "skutter.api.resilience", name = "circuit-breaker-enabled", havingValue = "true", matchIfMissing = true)
    public CircuitBreaker defaultCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker("default");
    }

    /**
     * Configure retry registry
     */
    @Bean
    @ConditionalOnProperty(prefix = "skutter.api.resilience", name = "retry-enabled", havingValue = "true", matchIfMissing = true)
    public RetryRegistry retryRegistry(SkutterApiProperties apiProperties) {
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(apiProperties.getResilience().getMaxRetryAttempts())
            .waitDuration(apiProperties.getResilience().getRetryBackoffDuration())
            .retryExceptions(Exception.class)
            .ignoreExceptions(IllegalArgumentException.class, IllegalStateException.class)
            .build();
        
        return RetryRegistry.of(retryConfig);
    }
} 