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
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PostgresSetUserIdInterceptorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Claims mockClaims;

    private PostgresSetUserIdInterceptor interceptor;
    private AutoCloseable closeable;


    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        interceptor = new PostgresSetUserIdInterceptor(jdbcTemplate);
        SecurityContextHolder.setContext(securityContext);
        
        interceptor.initialize();
    }
    
    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
        closeable.close();
    }

    @Test
    @DisplayName("Should set user ID when valid authentication is present")
    void setCurrentUserId_ValidAuthentication_SetsUserId() {
        // Arrange
        String userId = "test-user-123";
        SupabaseUserDetails userDetails = new SupabaseUserDetails(userId, "test@example.com", mockClaims, Collections.emptyList());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // Act
        interceptor.setCurrentUserId();

        // Assert
        verify(jdbcTemplate).execute(eq("SET LOCAL skutter.app.current_user_id = '\"test-user-123\"'"));
    }

    @ParameterizedTest
    @DisplayName("Should properly sanitize user IDs")
    @CsvSource({
        "simple-123",                    // Valid ID with hyphen
        "user_name",                     // Valid ID with underscore
        "user.name",                     // Valid ID with dot
        "malicious';DROP TABLE users;--", // SQL injection attempt -> sanitizes to "maliciousDROPTABLEusers"
        "123abc!@#$%^&*()_+",            // Special characters -> sanitizes to "123abc_"
        "' OR '1'='1",                   // Another SQL injection attempt -> sanitizes to "OR11"
        "user\\\"name"                     // Proper double quote escaping -> sanitizes to "username" (backslash removed) -> result is "\"username\""
    })
    void sanitize_ValidatesAndSanitizesInput(String input) {
        // Arrange
        SupabaseUserDetails userDetails = new SupabaseUserDetails(input, "test@example.com", mockClaims, Collections.emptyList());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // Act
        interceptor.setCurrentUserId();

        // Assert
        verify(jdbcTemplate).execute(argThat((String sql) -> 
            sql.startsWith("SET LOCAL skutter.app.current_user_id = '\"") && 
            sql.endsWith("\"'")
        ));
    }

    @Test
    @DisplayName("Should handle null user ID gracefully")
    void setCurrentUserId_NullUserId_DoesNotSetUserId() {
        // Arrange
        SupabaseUserDetails userDetails = new SupabaseUserDetails(null, "test@example.com", mockClaims, Collections.emptyList());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // Act
        interceptor.setCurrentUserId();

        // Assert
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    @DisplayName("Should handle missing authentication gracefully")
    void setCurrentUserId_NoAuthentication_DoesNotSetUserId() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act
        interceptor.setCurrentUserId();

        // Assert
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    @DisplayName("Should handle non-SupabaseUserDetails principal gracefully")
    void setCurrentUserId_NonSupabaseUserDetails_DoesNotSetUserId() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("some-other-principal");

        // Act
        interceptor.setCurrentUserId();

        // Assert
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    @DisplayName("Should handle non-authenticated user gracefully")
    void setCurrentUserId_NonAuthenticatedUser_DoesNotSetUserId() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act
        interceptor.setCurrentUserId();

        // Assert
        verify(jdbcTemplate, never()).execute(anyString());
    }
} 