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

package ai.skutter.common.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "skutter.security")
public class SkutterSecurityProperties {

    /**
     * Enable or disable security features
     */
    private boolean enabled = true;
    
    /**
     * JWT token configuration
     */
    private final Jwt jwt = new Jwt();
    
    /**
     * OAuth2 client configuration
     */
    private final OAuth2 oauth2 = new OAuth2();
    
    /**
     * CORS configuration
     */
    private final Cors cors = new Cors();
    
    /**
     * HTTPS configuration
     */
    private final Https https = new Https();
    
    /**
     * Paths that should be publicly accessible without authentication
     */
    private List<String> publicPaths = new ArrayList<>(List.of(
        "/actuator/health/**", 
        "/actuator/info",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    ));

    @Data
    public static class Jwt {
        /**
         * Secret key for JWT validation (if not using public key)
         */
        private String secret;
        
        /**
         * Public key path for JWT validation
         */
        private String publicKeyPath;
        
        /**
         * Issuer URL for JWT validation
         */
        private String issuer = "https://api.supabase.co/auth/v1";
        
        /**
         * Access token expiration time in milliseconds
         */
        private long expirationMs = 3600000;
        
        /**
         * Whether to validate JWT expiration
         */
        private boolean validateExpiration = true;
        
        /**
         * Whether to validate JWT issuer
         */
        private boolean validateIssuer = true;
        
        /**
         * JWT claim containing roles information
         */
        private String roleClaim = "app_metadata.skutter_role";
        
        /**
         * JWT claim containing user ID
         */
        private String userIdClaim = "sub";
        
        /**
         * Should the JWT user ID be set as a Postgres user ID via SET LOCAL
         */
        private boolean setPostgresUserId = true;
    }
    
    @Data
    public static class OAuth2 {
        /**
         * Resource server settings
         */
        private final ResourceServer resourceServer = new ResourceServer();
        
        @Data
        public static class ResourceServer {
            /**
             * Supabase JWT authentication settings
             */
            private final Supabase supabase = new Supabase();
            
            @Data
            public static class Supabase {
                /**
                 * Supabase URL
                 */
                private String url;
                
                /**
                 * Supabase API key
                 */
                private String apiKey;
                
                /**
                 * Supabase JWT secret
                 */
                private String jwtSecret;
            }
        }
    }
    
    @Data
    public static class Cors {
        /**
         * Whether CORS is enabled
         */
        private boolean enabled = true;
        
        /**
         * Allowed origins for CORS. Default is to allow all origins with "*".
         * For production, specify the exact domains.
         */
        private List<String> allowedOrigins = new ArrayList<>(List.of("*"));
        
        /**
         * Allowed methods for CORS
         */
        private List<String> allowedMethods = new ArrayList<>(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        /**
         * Allowed headers for CORS
         */
        private List<String> allowedHeaders = new ArrayList<>(List.of(
            "Authorization", "Content-Type", "Accept", "X-Requested-With", 
            "X-XSRF-TOKEN", "X-Tenant-ID"));
        
        /**
         * Whether to allow credentials for CORS
         */
        private boolean allowCredentials = true;
        
        /**
         * Max age in seconds for CORS preflight requests
         */
        private long maxAge = 3600;
    }
    
    @Data
    public static class Https {
        /**
         * Whether to enforce HTTPS for all requests
         */
        private boolean enabled = true;
        
        /**
         * Port to use for HTTPS
         */
        private int port = 8443;
        
        /**
         * Path to the keystore file
         */
        private String keystorePath = "classpath:keystore/keystore.p12";
        
        /**
         * Password for the keystore
         */
        private String keystorePassword = "";
        
        /**
         * Type of keystore (PKCS12, JKS, etc.)
         */
        private String keystoreType = "PKCS12";
        
        /**
         * Alias to use in the keystore
         */
        private String keystoreAlias = "";
        
        /**
         * Whether to enable HSTS (HTTP Strict Transport Security)
         */
        private boolean hstsEnabled = true;
        
        /**
         * Max age for HSTS in seconds (1 year by default)
         */
        private long hstsMaxAgeSeconds = 31536000;
        
        /**
         * Whether to include subdomains in HSTS
         */
        private boolean hstsIncludeSubDomains = true;
        
        /**
         * Whether to add the preload directive to HSTS
         */
        private boolean hstsPreload = false;
    }
} 