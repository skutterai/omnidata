package ai.skutter.common.api.ratelimit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;

/**
 * Token bucket implementation for rate limiting
 */
@Slf4j
public class RateLimitBucket {

    private final int capacity;
    private final Duration refillPeriod;
    
    @Getter
    private int tokens;
    private Instant lastRefillTime;

    /**
     * Create a rate limit bucket with the specified capacity and refill period
     */
    public RateLimitBucket(int capacity, Duration refillPeriod) {
        this.capacity = capacity;
        this.refillPeriod = refillPeriod;
        this.tokens = capacity;
        this.lastRefillTime = Instant.now();
        log.debug("Created new rate limit bucket with capacity={}, refillPeriod={}s", 
                 capacity, refillPeriod.getSeconds());
    }

    /**
     * Try to consume a token from the bucket
     * 
     * @return true if a token was consumed, false if no tokens are available
     */
    public boolean tryConsume() {
        refillIfNeeded();
        
        if (tokens > 0) {
            tokens--;
            log.trace("Token consumed. Remaining tokens: {}", tokens);
            return true;
        }
        
        log.debug("Token consumption failed - bucket empty. Next refill in {}s", getResetTimeSeconds());
        return false;
    }

    /**
     * Get the number of tokens remaining in the bucket
     */
    public int getRemaining() {
        refillIfNeeded();
        log.trace("Current remaining tokens: {}", tokens);
        return tokens;
    }

    /**
     * Get the time in seconds until the bucket will be refilled
     */
    public long getResetTimeSeconds() {
        Instant now = Instant.now();
        Instant nextRefillTime = lastRefillTime.plus(refillPeriod);
        
        if (now.isAfter(nextRefillTime)) {
            log.trace("Bucket is ready for immediate refill");
            return 0;
        }
        
        long seconds = Duration.between(now, nextRefillTime).getSeconds();
        log.trace("Next refill in {} seconds", seconds);
        return seconds;
    }

    /**
     * Refill the bucket if the refill period has elapsed
     */
    private void refillIfNeeded() {
        Instant now = Instant.now();
        long elapsedTime = Duration.between(lastRefillTime, now).toMillis();
        long refillTimeMillis = refillPeriod.toMillis();
        
        if (elapsedTime >= refillTimeMillis) {
            // Calculate how many refill periods have passed
            int periods = (int) (elapsedTime / refillTimeMillis);
            
            // Refill the tokens
            int oldTokens = tokens;
            tokens = capacity;
            
            // Update the last refill time
            lastRefillTime = now;
            
            log.debug("Bucket refilled. Old tokens: {}, New tokens: {}, Periods elapsed: {}", 
                     oldTokens, tokens, periods);
        }
    }
} 