package ai.skutter.common.client;

import ai.skutter.common.observability.filter.CorrelationIdFilter;
import ai.skutter.common.observability.properties.SkutterObservabilityProperties;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * RestTemplate interceptor to add the current Correlation ID to outgoing requests.
 */
public class CorrelationIdClientInterceptor implements ClientHttpRequestInterceptor {

    private final SkutterObservabilityProperties properties;

    public CorrelationIdClientInterceptor(SkutterObservabilityProperties properties) {
        this.properties = properties;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        if (correlationId != null && properties.getCorrelation().isPropagateToDownstream()) {
            request.getHeaders().add(properties.getCorrelation().getHeaderName(), correlationId);
        }
        return execution.execute(request, body);
    }
} 