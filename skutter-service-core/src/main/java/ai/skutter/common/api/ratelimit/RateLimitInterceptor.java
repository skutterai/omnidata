package ai.skutter.common.api.ratelimit;

import ai.skutter.common.api.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interceptor for rate limiting incoming requests
 */
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final int limit;
    private final Duration refreshPeriod;
    private final Map<String, RateLimitBucket> buckets;

    public RateLimitInterceptor(int limit, Duration refreshPeriod) {
        this.limit = limit;
        this.refreshPeriod = refreshPeriod;
        this.buckets = new ConcurrentHashMap<>();
        log.info("Initialized RateLimitInterceptor with limit={} requests per {} seconds", 
                limit, refreshPeriod.getSeconds());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        log.debug("Processing rate limit check for path: {}", path);
        
        // Skip rate limiting for certain endpoints
        if (shouldSkipRateLimit(request)) {
            log.trace("Skipping rate limit for path: {}", path);
            return true;
        }
        
        String clientIp = getClientIp(request);
        log.debug("Rate limit check for client IP: {}", clientIp);
        
        RateLimitBucket bucket = getBucket(clientIp);
        
        synchronized (bucket) {
            // Check if rate limit is exceeded
            if (!bucket.tryConsume()) {
                log.warn("Rate limit exceeded for client IP: {} on path: {}", clientIp, path);
                throw new RateLimitExceededException(
                    "Rate limit exceeded. Maximum allowed: " + limit + " requests per " + refreshPeriod.getSeconds() + " seconds");
            }
            
            // Add rate limit headers
            int remaining = bucket.getRemaining();
            long resetTime = bucket.getResetTimeSeconds();
            
            response.addHeader("X-Rate-Limit-Limit", String.valueOf(limit));
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(remaining));
            response.addHeader("X-Rate-Limit-Reset", String.valueOf(resetTime));
            
            log.trace("Request allowed. Remaining tokens: {}, Reset in: {}s", remaining, resetTime);
            return true;
        }
    }

    /**
     * Get the rate limit bucket for a client IP
     */
    private RateLimitBucket getBucket(String clientIp) {
        return buckets.computeIfAbsent(clientIp, ip -> {
            log.debug("Creating new rate limit bucket for client IP: {}", ip);
            return new RateLimitBucket(limit, refreshPeriod);
        });
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String remoteAddr = request.getRemoteAddr();
        
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Get the first IP in the chain (client IP)
            String clientIp = xForwardedFor.split(",")[0].trim();
            log.trace("Using X-Forwarded-For IP: {} (original remote: {})", clientIp, remoteAddr);
            return clientIp;
        }
        
        log.trace("Using remote address IP: {}", remoteAddr);
        return remoteAddr;
    }

    /**
     * Check if rate limiting should be skipped for this request
     */
    private boolean shouldSkipRateLimit(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip actuator endpoints, swagger, and health checks
        boolean shouldSkip = path.startsWith("/actuator") || 
                           path.startsWith("/v3/api-docs") || 
                           path.startsWith("/swagger-ui") ||
                           path.equals("/health");
        
        if (shouldSkip) {
            log.trace("Skipping rate limit for path: {}", path);
        }
        
        return shouldSkip;
    }
} 