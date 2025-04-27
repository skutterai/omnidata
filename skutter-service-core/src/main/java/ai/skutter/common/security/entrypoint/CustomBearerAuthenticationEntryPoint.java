package ai.skutter.common.security.entrypoint;

import ai.skutter.common.api.model.ApiErrorResponse;
import ai.skutter.common.observability.filter.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Custom AuthenticationEntryPoint to return a standardized API error response for 401 Unauthorized.
 */
@Component // Register as a bean
public class CustomBearerAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String ERROR_DOCS_BASE_URL = "https://api.skutter.ai/docs/errors"; // Keep consistent

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        String correlationId = request.getHeader(CorrelationIdFilter.CORRELATION_ID_KEY);
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        String message = "Authentication required. Please provide a valid Bearer token.";
        String details = authException.getMessage(); // Provide original exception message as details
        String errorCode = "authentication_required";
        
        ApiErrorResponse.ErrorDetails errorDetails = ApiErrorResponse.ErrorDetails.builder()
                .code(errorCode)
                .message(message)
                .details(details)
                .requestId(correlationId)
                .path(request.getRequestURI())
                .build();
                
        ApiErrorResponse apiErrorResponse = ApiErrorResponse.builder().error(errorDetails).build();

        response.setStatus(status.value());
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"api\"");
        if (correlationId != null) {
            response.setHeader(CorrelationIdFilter.CORRELATION_ID_KEY, correlationId);
        }
        // Add Link header
        String linkUrl = String.format("%s/%d", ERROR_DOCS_BASE_URL, status.value());
        response.setHeader(HttpHeaders.LINK, String.format("<%s>; rel=\"help\"", linkUrl));

        // Write the JSON response body
        response.getWriter().write(objectMapper.writeValueAsString(apiErrorResponse));
    }
} 