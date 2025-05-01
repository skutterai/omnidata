package ai.skutter.common.api.ratelimit;

import ai.skutter.common.api.exception.RateLimitExceededException;
import ai.skutter.common.security.jwt.SupabaseUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interceptor for rate limiting incoming requests based on IP address or authenticated user.
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
        log.info("Initializing RateLimitInterceptor: Limit={} requests per {} seconds", 
                limit, refreshPeriod.getSeconds());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.trace("RateLimitInterceptor preHandle executing for request: {} {}", request.getMethod(), request.getRequestURI());

        String key = resolveKey(request);
        log.trace("Rate limiting key resolved to: {}", key);

        RateLimitBucket bucket = buckets.computeIfAbsent(key, k -> {
            log.debug("Creating new rate limit bucket for key: {}", k);
            return new RateLimitBucket(limit, refreshPeriod);
        });

        boolean allowed = bucket.tryConsume();
        if (allowed) {
            log.trace("Request allowed for key: {}. Tokens remaining: {}", key, bucket.getAvailableTokens());
            return true;
        } else {
            log.warn("Rate limit exceeded for key: {}. Request denied: {} {}", key, request.getMethod(), request.getRequestURI());
            response.addHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            response.addHeader("X-RateLimit-Retry-After-Seconds", String.valueOf(bucket.getSecondsUntilNextRefill()));
            
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later."); 
        }
    }

    /**
     * Resolves the key for rate limiting. Uses the stable user ID from SupabaseUserDetails
     * if available, otherwise falls back to IP address.
     * 
     * @param request The incoming HTTP request.
     * @return The key used for rate limiting.
     */
    private String resolveKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            
            // Check if the principal is our custom user details object
            if (principal instanceof SupabaseUserDetails) {
                String userId = ((SupabaseUserDetails) principal).getUserId();
                // Ensure userId is not empty before using it
                if (StringUtils.hasText(userId)) {
                     log.trace("Using authenticated Supabase User ID for rate limiting: {}", userId);
                     return "user-" + userId; // Use the stable user ID
                } else {
                     log.warn("Authenticated user principal (SupabaseUserDetails) has empty/null user ID. Falling back.");
                }
            } else {
                 // Principal might be a String or something else
                 log.trace("Principal is not SupabaseUserDetails (type: {}). Using authentication name as fallback.", 
                           principal != null ? principal.getClass().getName() : "null");
            }
             
            // Fallback for authenticated users where specific ID extraction failed or wasn't possible
            String authName = authentication.getName(); 
            if (authName != null && !"anonymousUser".equals(authName)) {
                log.trace("Using authentication name as fallback for rate limiting: {}", authName);
                // Be aware this might still be unstable if authName is based on toString()
                return "user-" + authName; 
            }
        } 
        
        // Fallback to IP address for unauthenticated or anonymous users
        String ipAddress = request.getRemoteAddr();
        log.trace("Using IP address for rate limiting: {}", ipAddress);
        return "ip-" + ipAddress;
    }

    private static class RateLimitBucket {
        private final int capacity;
        private final Duration refillPeriod;
        private final long refillTokens;
        private long availableTokens;
        private long lastRefillTimestamp;

        public RateLimitBucket(int capacity, Duration refillPeriod) {
            this.capacity = capacity;
            this.refillPeriod = refillPeriod;
            this.refillTokens = capacity;
            this.availableTokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
             log.trace("Bucket created: capacity={}, refillPeriod={}s", capacity, refillPeriod.getSeconds());
        }

        public synchronized boolean tryConsume() {
            refill();
            if (availableTokens > 0) {
                availableTokens--;
                 log.trace("Token consumed. Remaining: {}", availableTokens);
                return true;
            } else {
                 log.trace("No tokens available to consume.");
                return false;
            }
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long durationSinceLastRefill = now - lastRefillTimestamp;
            long periodsPassed = durationSinceLastRefill / refillPeriod.toMillis();

            if (periodsPassed > 0) {
                long tokensToAdd = periodsPassed * refillTokens;
                this.availableTokens = Math.min(capacity, availableTokens + tokensToAdd);
                this.lastRefillTimestamp = now;
                 log.trace("Refilled {} tokens. New balance: {}. Periods passed: {}", tokensToAdd, availableTokens, periodsPassed);
            }
        }
        
        public synchronized long getAvailableTokens() {
            refill();
            return availableTokens;
        }

        public synchronized long getSecondsUntilNextRefill() {
            long now = System.currentTimeMillis();
            long millisSinceLastRefill = now - lastRefillTimestamp;
            long millisToNextRefill = refillPeriod.toMillis() - (millisSinceLastRefill % refillPeriod.toMillis());
            return Math.max(0, millisToNextRefill / 1000);
        }
    }
} 