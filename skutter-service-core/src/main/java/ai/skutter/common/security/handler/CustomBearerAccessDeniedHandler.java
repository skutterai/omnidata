package ai.skutter.common.security.handler;

import ai.skutter.common.api.model.ApiErrorResponse;
import ai.skutter.common.observability.filter.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Custom AccessDeniedHandler to return a standardized API error response for 403 Forbidden.
 */
@Component // Register as a bean
public class CustomBearerAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String ERROR_DOCS_BASE_URL = "https://api.skutter.ai/docs/errors"; // Keep consistent


    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        String correlationId = request.getHeader(CorrelationIdFilter.CORRELATION_ID_KEY);
        HttpStatus status = HttpStatus.FORBIDDEN;
        String message = "Access Denied. You do not have sufficient permissions to access this resource.";
        String details = accessDeniedException.getMessage(); // Provide original exception message as details
        String errorCode = "insufficient_permissions";

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