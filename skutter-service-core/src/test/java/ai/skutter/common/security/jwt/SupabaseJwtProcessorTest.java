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
package ai.skutter.common.security.jwt;

import ai.skutter.common.security.properties.SkutterSecurityProperties;
import ai.skutter.common.security.role.SkutterRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SupabaseJwtProcessorTest {

    private SkutterSecurityProperties securityProperties;
    private JwtTokenProvider tokenProvider;
    private SupabaseJwtProcessor jwtProcessor;
    // Use a PLAIN TEXT secret string for testing
    private final String plainTextSecret = "TestSecretKeyMustBeLongEnoughForHS256Algorithm"; // Ensure sufficient length
    private String testIssuer = "test-issuer";
    private String testUserId = "eeb624e7-2ff9-460d-8678-03f61f914765";
    private String testRole = "PLATFORM_OWNER";
    private String expectedAuthority = "ROLE_PLATFORM_OWNER";
    private long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        securityProperties = new SkutterSecurityProperties();
        SkutterSecurityProperties.Jwt jwtProps = securityProperties.getJwt(); 

        // Set the PLAIN TEXT secret string in the properties
        // Assuming JwtTokenProvider uses this directly or hashes it for HS256 key
        jwtProps.setSecret(plainTextSecret);
        
        jwtProps.setIssuer(testIssuer);
        jwtProps.setRoleClaim("app_metadata.skutter_role"); 
        jwtProps.setUserIdClaim("sub"); 
        jwtProps.setValidateIssuer(true); 
        jwtProps.setValidateExpiration(true); 
        jwtProps.setExpirationMs(expirationMs);

        tokenProvider = new JwtTokenProvider(securityProperties);
        tokenProvider.init(); 

        jwtProcessor = new SupabaseJwtProcessor(tokenProvider);
    }

    private SecretKey getSigningKey() {
        // Derive the signing key directly from the plain text secret string
        byte[] keyBytes = plainTextSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Test
    @DisplayName("Should process valid JWT with PLATFORM_OWNER role and extract correct UserDetails")
    void processToken_PlatformOwnerRole_ExtractsUserDetails() {
        // Create proper nested structure for app_metadata
        Map<String, Object> appMetadata = new HashMap<>();
        appMetadata.put("skutter_role", testRole);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        SecretKey signingKey = getSigningKey(); // Use key derived from plain text

        String token = Jwts.builder()
                .setSubject(testUserId)
                .setIssuer(testIssuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setAudience("authenticated") 
                .claim("app_metadata", appMetadata) // This sets app_metadata as a Map
                .claim("email", "test@example.com")
                .signWith(signingKey) // Sign with the key derived from plain text
                .compact();

        Authentication authentication = jwtProcessor.process(token);

        assertNotNull(authentication, "Authentication object should not be null");
        Object principal = authentication.getPrincipal();
        assertNotNull(principal, "Principal object should not be null");
        if (!(principal instanceof SupabaseUserDetails)) {
            System.err.println("Unexpected Principal Type: " + principal.getClass().getName());
            System.err.println("Principal Details: " + principal);
        }
        assertTrue(principal instanceof SupabaseUserDetails, "Principal should be SupabaseUserDetails");
        
        SupabaseUserDetails userDetails = (SupabaseUserDetails) authentication.getPrincipal();

        assertEquals(testUserId, userDetails.getUserId(), "User ID should match the 'sub' claim");
        assertNotNull(userDetails.getAuthorities(), "Authorities list should not be null");
        
        // Check for ROLE_ prefix authority
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority(expectedAuthority)),
                "Authorities should contain " + expectedAuthority);
                
        // Check for permissions and scope authorities
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("PERMISSION_READ")),
                "Authorities should contain READ permission");
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("PERMISSION_WRITE")),
                "Authorities should contain WRITE permission");
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("SCOPE_PLATFORM")),
                "Authorities should contain PLATFORM scope");
                
        // Verify helper methods
        assertTrue(userDetails.hasPlatformAccess(), "Platform owner should have platform access");
        assertTrue(userDetails.canWrite(), "Platform owner should have write access");
        assertTrue(userDetails.hasRole(SkutterRole.PLATFORM_OWNER), "Platform owner should have PLATFORM_OWNER role");
    }
    
    @Test
    @DisplayName("Should process valid JWT with PROJECT_ADMIN role")
    void processToken_ProjectAdminRole_ExtractsCorrectPermissions() {
        // Create proper nested structure for app_metadata
        Map<String, Object> appMetadata = new HashMap<>();
        appMetadata.put("skutter_role", "PROJECT_ADMIN");
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        SecretKey signingKey = getSigningKey();

        String token = Jwts.builder()
                .setSubject(testUserId)
                .setIssuer(testIssuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setAudience("authenticated") 
                .claim("app_metadata", appMetadata)
                .claim("email", "test@example.com")
                .signWith(signingKey)
                .compact();

        Authentication authentication = jwtProcessor.process(token);

        assertNotNull(authentication, "Authentication object should not be null");
        assertTrue(authentication.getPrincipal() instanceof SupabaseUserDetails, "Principal should be SupabaseUserDetails");
        
        SupabaseUserDetails userDetails = (SupabaseUserDetails) authentication.getPrincipal();
        
        // Check for role and permissions
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PROJECT_ADMIN")),
                "Authorities should contain PROJECT_ADMIN role");
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("PERMISSION_READ")),
                "Authorities should contain READ permission");
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("PERMISSION_WRITE")),
                "Authorities should contain WRITE permission");
                
        // Verify helper methods
        assertFalse(userDetails.hasPlatformAccess(), "Project admin should not have platform access");
        assertTrue(userDetails.canWrite(), "Project admin should have write access");
        assertTrue(userDetails.hasRole(SkutterRole.PROJECT_ADMIN), "Project admin should have PROJECT_ADMIN role");
    }

    @Test
    @DisplayName("Should return null for invalid token (e.g., wrong signature)")
    void processToken_InvalidSignature_ReturnsNull() {
        SecretKey wrongKey = Keys.secretKeyFor(SignatureAlgorithm.HS256); // A different key
        Map<String, Object> appMetadata = new HashMap<>();
        appMetadata.put("skutter_role", testRole);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        String token = Jwts.builder()
                .setSubject(testUserId)
                .setIssuer(testIssuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("app_metadata", appMetadata)
                .signWith(wrongKey) // Sign with a different key
                .compact();

        Authentication authentication = jwtProcessor.process(token);

        assertNull(authentication, "Authentication should be null for invalid signature");
    }
    
    @Test
    @DisplayName("Should process token with PROJECT_VIEWER role correctly")
    void processToken_ProjectViewerRole_ExtractsCorrectPermissions() {
        // Create proper nested structure for app_metadata
        Map<String, Object> appMetadata = new HashMap<>();
        appMetadata.put("skutter_role", "PROJECT_VIEWER");
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        SecretKey signingKey = getSigningKey();

        String token = Jwts.builder()
                .setSubject(testUserId)
                .setIssuer(testIssuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("app_metadata", appMetadata)
                .signWith(signingKey)
                .compact();

        Authentication authentication = jwtProcessor.process(token);

        assertNotNull(authentication, "Authentication object should not be null");
        SupabaseUserDetails userDetails = (SupabaseUserDetails) authentication.getPrincipal();
        
        // Check for role and permissions
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PROJECT_VIEWER")),
                "Authorities should contain PROJECT_VIEWER role");
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("PERMISSION_READ")),
                "Authorities should contain READ permission");
        assertFalse(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("PERMISSION_WRITE")),
                "Authorities should NOT contain WRITE permission");
                
        // Verify helper methods
        assertFalse(userDetails.hasPlatformAccess(), "Project viewer should not have platform access");
        assertFalse(userDetails.canWrite(), "Project viewer should not have write access");
        assertTrue(userDetails.canRead(), "Project viewer should have read access");
        assertTrue(userDetails.hasRole(SkutterRole.PROJECT_VIEWER), "Should have PROJECT_VIEWER role");
    }
    
    @Test
    @DisplayName("Should reject authentication when role claim is missing")
    void processToken_MissingRoleClaim_ReturnsNullAuthentication() {
        Map<String, Object> appMetadata = new HashMap<>();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        SecretKey signingKey = getSigningKey(); // Use correct key

        String token = Jwts.builder()
                .setSubject(testUserId)
                .setIssuer(testIssuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("app_metadata", appMetadata)
                .signWith(signingKey)
                .compact();

        Authentication authentication = jwtProcessor.process(token);

        assertNull(authentication, "Authentication should be null when role claim is missing");
    }
} 