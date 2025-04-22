package ai.skutter.common.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;

/**
 * RestTemplate interceptor to propagate the Authorization header (JWT) from the
 * incoming request to the outgoing request, if available.
 */
@Slf4j
@RequiredArgsConstructor
public class SecurityContextClientInterceptor implements ClientHttpRequestInterceptor {

    private static final String AUTHORIZATION_HEADER = HttpHeaders.AUTHORIZATION;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        
        // Try to get the Authorization header from the current request context
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest currentRequest = attributes.getRequest();
            String authorizationHeader = currentRequest.getHeader(AUTHORIZATION_HEADER);
            
            if (authorizationHeader != null && !request.getHeaders().containsKey(AUTHORIZATION_HEADER)) {
                log.debug("Propagating Authorization header to downstream request");
                request.getHeaders().add(AUTHORIZATION_HEADER, authorizationHeader);
            }
        }
        
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            request.getHeaders().add("Authorization", "Bearer " + jwt.getTokenValue());
        }
        
        return execution.execute(request, body);
    }
} 