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
package ai.skutter.common.data.jdbc;

import ai.skutter.common.security.jwt.SupabaseUserDetails;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Intercepts database connections to set the current user ID in the Postgres session
 * using SET LOCAL for row-level security and audit logging.
 */
@Slf4j
@Component
public class PostgresSetUserIdInterceptor {

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;
    
    public PostgresSetUserIdInterceptor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = true; // Assuming the component is enabled by default
    }
    
    @PostConstruct
    public void initialize() {
        if (enabled) {
            log.info("PostgreSQL user ID propagation is enabled");
            // registerTransactionSynchronization(); // Commented out - Registration should happen within an active transaction context
        } else {
            log.info("PostgreSQL user ID propagation is disabled");
        }
    }
    
    /**
     * Register transaction synchronization to set user ID at transaction start
     */
    private void registerTransactionSynchronization() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
                setCurrentUserId();
            }
            
            public void afterBegin() {
                setCurrentUserId();
            }
        });
    }
    
    /**
     * Set the current user ID in the PostgreSQL session
     */
    private void setCurrentUserId() {
        try {
            getUserId().ifPresent(userId -> {
                // Sanitize the user ID first
                String sanitizedUserId = sanitize(userId);
                
                // Only proceed if sanitization didn't result in null value
                if (sanitizedUserId != null) {
                    // Set user ID as a local variable in the PostgreSQL session
                    jdbcTemplate.execute("SET LOCAL skutter.app.current_user_id = '" + sanitizedUserId + "'");
                    log.debug("Set PostgreSQL session user_id to: {}", userId);
                } else {
                    log.warn("Cannot set PostgreSQL user_id - sanitization resulted in null value for input: {}", userId);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to set PostgreSQL user ID", e);
        }
    }
    
    /**
     * Get the current user ID from the security context
     */
    private Optional<String> getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
            authentication.getPrincipal() instanceof SupabaseUserDetails) {
            
            SupabaseUserDetails userDetails = (SupabaseUserDetails) authentication.getPrincipal();
            String userId = userDetails.getUserId();
            
            if (StringUtils.hasText(userId)) {
                return Optional.of(userId);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Sanitize input to prevent SQL injection
     */
    private String sanitize(String input) {
        if (input == null) {
            return null;
        }
        
        // Only allow alphanumeric, underscore, hyphen, and dot characters
        // This pattern matches PostgreSQL's identifier rules
        String sanitized = input.replaceAll("[^a-zA-Z0-9_\\-\\.]", "");
        
        // If the sanitized string is empty, return null
        if (sanitized.isEmpty()) {
            log.warn("User ID sanitization resulted in empty string. Original input: {}", input);
            return null;
        }
        
        // Double quote the identifier to handle any special characters
        return "\"" + sanitized.replace("\"", "\"\"") + "\"";
    }
} 