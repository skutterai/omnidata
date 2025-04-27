package ai.skutter.common.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception to indicate that a requested resource was not found.
 * Typically results in an HTTP 404 Not Found response.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND) // Annotate for automatic status code mapping if not caught by handler
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s not found with ID: %s", resourceType, resourceId));
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 