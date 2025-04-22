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

package ai.skutter.common.security.logging;

import ai.skutter.common.security.jwt.SupabaseUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter for logging security-related information about requests and responses
 */
@Slf4j
public class SecurityLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
            
        ContentCachingRequestWrapper requestWrapper = wrapRequest(request);
        ContentCachingResponseWrapper responseWrapper = wrapResponse(response);
        
        try {
            // Handle request
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            // Log authentication information
            logAuthenticationDetails(requestWrapper);
            
            // Always copy response content
            responseWrapper.copyBodyToResponse();
        }
    }

    /**
     * Wrap the request to be able to read its content multiple times
     */
    private ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            return (ContentCachingRequestWrapper) request;
        }
        return new ContentCachingRequestWrapper(request);
    }

    /**
     * Wrap the response to be able to read its content multiple times
     */
    private ContentCachingResponseWrapper wrapResponse(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            return (ContentCachingResponseWrapper) response;
        }
        return new ContentCachingResponseWrapper(response);
    }

    /**
     * Log authentication details for debugging
     */
    private void logAuthenticationDetails(HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                log.debug("Request: {} {} - User: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    getUserDetails(authentication)
                        .map(SupabaseUserDetails::getUserId)
                        .orElse("unknown")
                );
            } else {
                log.debug("Request: {} {} - Unauthenticated",
                    request.getMethod(),
                    request.getRequestURI());
            }
        }
    }

    /**
     * Extract user details from authentication if possible
     */
    private Optional<SupabaseUserDetails> getUserDetails(Authentication authentication) {
        if (authentication.getPrincipal() instanceof SupabaseUserDetails) {
            return Optional.of((SupabaseUserDetails) authentication.getPrincipal());
        }
        return Optional.empty();
    }
} 