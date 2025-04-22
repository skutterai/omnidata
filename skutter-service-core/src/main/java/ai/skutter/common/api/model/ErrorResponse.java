package ai.skutter.common.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response format
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Timestamp when the error occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * HTTP status code
     */
    private int status;
    
    /**
     * Error code for machine-readable error identification
     */
    private String error;
    
    /**
     * Human-readable error message
     */
    private String message;
    
    /**
     * Correlation ID for tracing requests across services
     */
    private String correlationId;
    
    /**
     * Validation errors as field-message pairs
     */
    private Map<String, String> errors;

    /**
     * Path of the request that caused the error
     */ 
    private String path;
} 