package ai.skutter.common.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized API error response wrapper containing error details.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private ErrorDetails error;

    /**
     * Inner class holding the specific error details.
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        private String code;      // e.g., "insufficient_permissions", "validation_error"
        private String message;   // e.g., "Permission denied", "Invalid request data"
        private String details;   // Optional, e.g., "Requires scope: read:items", "Field 'x' must not be null"
        private String requestId; // Correlation ID
        private String path;      // Request URI
        // Optional: Add timestamp here if preferred over top-level
        // private LocalDateTime timestamp; 
        // Optional: Add validation errors map here if preferred
        // private Map<String, String> errors; 
    }
} 