package ai.skutter.common.api.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

/**
 * Configuration properties for API features.
 * NOTE: This is kept for backward compatibility - prefer standard Spring properties where possible.
 */
@Data
@ConfigurationProperties(prefix = "skutter.api")
public class SkutterApiProperties {

    /**
     * Rate limiting configuration.
     */
    private final RateLimit rateLimit = new RateLimit();

    /**
     * API Documentation configuration.
     */
    private final Documentation documentation = new Documentation();

    /**
     * Resilience configuration.
     */
    private final Resilience resilience = new Resilience();

    @Data
    public static class RateLimit {
        /**
         * Enable or disable rate limiting.
         */
        private boolean enabled = true;

        /**
         * Maximum requests per refresh period.
         */
        private int limit = 100;

        /**
         * Refresh period.
         */
        private Duration refreshPeriod = Duration.ofMinutes(1);
    }

    @Data
    public static class Documentation {
        /**
         * Enable or disable API documentation.
         */
        private boolean enabled = true;

        /**
         * API title.
         */
        private String title = "Skutter Service API";

        /**
         * API version.
         */
        private String version = "1.0";

        /**
         * API description.
         */
        private String description = "API for Skutter Service";

        /**
         * Contact name.
         */
        private String contactName = "Skutter Development Team";

        /**
         * Contact email.
         */
        private String contactEmail = "dev@skutter.ai";

        /**
         * Contact URL.
         */
        private String contactUrl = "https://skutter.ai";

        /**
         * License name.
         */
        private String licenseName = "Copyright Skutter.ai";

        /**
         * License URL.
         */
        private String licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.html";
    }

    @Data
    public static class Resilience {
        /**
         * Enable or disable circuit breaker.
         */
        private boolean circuitBreakerEnabled = true;

        /**
         * Failure threshold percentage.
         */
        private int failureThreshold = 50;

        /**
         * Wait duration in open state.
         */
        private Duration waitDurationInOpenState = Duration.ofSeconds(60);

        /**
         * Enable or disable retry.
         */
        private boolean retryEnabled = true;

        /**
         * Maximum retry attempts.
         */
        private int maxRetryAttempts = 3;

        /**
         * Retry backoff duration.
         */
        private Duration retryBackoffDuration = Duration.ofMillis(500);
    }
} 