/*
 * Copyright (c) 2025 Skutter.ai
 *
 * This code is proprietary and confidential. Unauthorized copying, modification,
 * distribution, or use of this software, via any medium is strictly prohibited.
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * @author mattduggan
 */
package ai.skutter.common.observability.filter;

import ai.skutter.common.observability.properties.SkutterObservabilityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that manages correlation IDs for request tracking across services
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_KEY = "correlationId";

    private final SkutterObservabilityProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            String correlationId = getCorrelationId(request);
            MDC.put(CORRELATION_ID_KEY, correlationId);
            response.setHeader(properties.getCorrelation().getHeaderName(), correlationId);
            
            log.debug("Processing request: {} {} with correlation ID: {}", 
                     request.getMethod(), 
                     request.getRequestURI(), 
                     correlationId);
                
            filterChain.doFilter(request, response);
            
        } finally {
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    /**
     * Extract correlation ID from request header or generate a new one if needed
     */
    private String getCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(properties.getCorrelation().getHeaderName());
        
        if (correlationId == null && properties.getCorrelation().isGenerateIfMissing()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        return correlationId;
    }
} 