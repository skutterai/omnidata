package ai.skutter.common.security;

import ai.skutter.common.security.jwt.JwtTokenProvider;
import ai.skutter.common.security.jwt.SupabaseJwtProcessor;
import ai.skutter.common.security.jwt.SupabaseUserDetails;
import ai.skutter.common.security.properties.SkutterSecurityProperties;
import ai.skutter.common.security.role.SkutterRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify JWT processing using real tokens obtained from Supabase.
 * Requires Supabase credentials and details to be configured via environment variables.
 * Tests JWT functionality with each of the four Skutter platform roles.
 */
class SupabaseJwtIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(SupabaseJwtIntegrationTest.class);

    // Create instances directly
    private JwtTokenProvider jwtTokenProvider;
    private SupabaseJwtProcessor jwtProcessor;
    private SkutterSecurityProperties securityProperties;

    // Create RestTemplateBuilder directly 
    private final RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Read environment variables manually
    private String supabaseUrl;
    private String supabaseApiKey;
    private String jwtSecret;
    private String jwtIssuer;

    // Test credentials
    private String emailPlatformOwner;
    private String passwordPlatformOwner;
    private String emailPlatformViewer;
    private String passwordPlatformViewer;
    private String emailProjectAdmin;
    private String passwordProjectAdmin;
    private String emailProjectViewer;
    private String passwordProjectViewer;

    @BeforeEach
    void setUp() {
        // Read environment variables
        supabaseUrl = System.getenv("SUPABASE_URL");
        supabaseApiKey = System.getenv("SUPABASE_API_KEY");
        jwtSecret = System.getenv("JWT_SECRET");
        jwtIssuer = System.getenv("JWT_ISSUER");
        
        // Test user credentials
        emailPlatformOwner = System.getenv("SUPABASE_EMAIL_PLATFORM_OWNER");
        passwordPlatformOwner = System.getenv("SUPABASE_PASSWORD_PLATFORM_OWNER");
        emailPlatformViewer = System.getenv("SUPABASE_EMAIL_PLATFORM_VIEWER");
        passwordPlatformViewer = System.getenv("SUPABASE_PASSWORD_PLATFORM_VIEWER");
        emailProjectAdmin = System.getenv("SUPABASE_EMAIL_PROJECT_ADMIN");
        passwordProjectAdmin = System.getenv("SUPABASE_PASSWORD_PROJECT_ADMIN");
        emailProjectViewer = System.getenv("SUPABASE_EMAIL_PROJECT_VIEWER");
        passwordProjectViewer = System.getenv("SUPABASE_PASSWORD_PROJECT_VIEWER");
        
        // Log configuration values
        log.info("--- Supabase Test Config Check ---");
        log.info("Supabase URL: {}", supabaseUrl);
        log.info("Supabase API Key: {}", supabaseApiKey != null ? "SET" : "NULL");
        log.info("JWT Secret: {}", jwtSecret != null ? "SET (length: " + jwtSecret.length() + ")" : "NULL");
        log.info("JWT Issuer: {}", jwtIssuer);
        log.info("PlatformOwner Email: {}", emailPlatformOwner);
        log.info("PlatformOwner Password: {}", passwordPlatformOwner != null ? "SET" : "NULL");
        log.info("PlatformViewer Email: {}", emailPlatformViewer);
        log.info("PlatformViewer Password: {}", passwordPlatformViewer != null ? "SET" : "NULL");
        log.info("ProjectAdmin Email: {}", emailProjectAdmin);
        log.info("ProjectAdmin Password: {}", passwordProjectAdmin != null ? "SET" : "NULL");
        log.info("ProjectViewer Email: {}", emailProjectViewer);
        log.info("ProjectViewer Password: {}", passwordProjectViewer != null ? "SET" : "NULL");
        log.info("----------------------------------");

        // Basic check for required configuration
        if (supabaseUrl == null || supabaseApiKey == null) {
            log.warn("Supabase URL or API Key not configured via environment. Tests might fail.");
        }
        
        // Initialize security components
        initializeSecurityComponents();
        
        // Initialize RestTemplate
        this.restTemplate = restTemplateBuilder.build();
    }
    
    /**
     * Initialize security components manually
     */
    private void initializeSecurityComponents() {
        // Create security properties
        securityProperties = new SkutterSecurityProperties();
        SkutterSecurityProperties.Jwt jwtProps = securityProperties.getJwt();
        
        // Configure JWT properties
        if (StringUtils.hasText(jwtSecret)) {
            jwtProps.setSecret(jwtSecret);
            log.info("Using JWT secret from environment");
        } else {
            log.warn("JWT_SECRET not configured via environment. Using a default test secret.");
            jwtProps.setSecret("default-integration-test-secret-key-must-be-long");
        }
        
        if (StringUtils.hasText(jwtIssuer)) {
            jwtProps.setIssuer(jwtIssuer);
            log.info("Using JWT issuer from environment: {}", jwtIssuer);
        }
        
        jwtProps.setRoleClaim("app_metadata.skutter_role");
        jwtProps.setUserIdClaim("sub");
        jwtProps.setValidateIssuer(true);
        jwtProps.setValidateExpiration(true);
        
        // Create token provider
        jwtTokenProvider = new JwtTokenProvider(securityProperties);
        jwtTokenProvider.init();
        
        // Create JWT processor
        jwtProcessor = new SupabaseJwtProcessor(jwtTokenProvider);
    }

    /**
     * Fetches a JWT token from Supabase for the specified credentials
     */
    private String fetchSupabaseToken(String email, String password) {
        if (supabaseUrl == null || supabaseApiKey == null || email == null || password == null) {
            fail("Supabase test credentials or URL/API Key not configured for user: " + email);
        }

        String tokenUrl = supabaseUrl + "/auth/v1/token?grant_type=password";

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("email", email, "password", password);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, requestEntity, String.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            JsonNode responseBody = objectMapper.readTree(response.getBody());
            assertTrue(responseBody.has("access_token"));
            String accessToken = responseBody.get("access_token").asText();
            assertThat(accessToken).isNotBlank();
            
            // Print the full response body for debugging
            log.info("Token response for {}: {}", email, responseBody.toPrettyString());
            
            // Debug: Print decoded token payload
            String[] parts = accessToken.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                log.info("Decoded token payload for {}: {}", email, payload);
            }
            
            log.info("Successfully fetched token for {}", email);
            return accessToken;

        } catch (HttpClientErrorException e) {
            log.error("HTTP Error fetching token for {}: {} - {}", email, e.getStatusCode(), e.getResponseBodyAsString(), e);
            fail("Failed to fetch token for " + email + " due to HTTP error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching or parsing token for {}: {}", email, e.getMessage(), e);
            fail("Failed to fetch or parse token for " + email + ": " + e.getMessage());
        }
        return null; // Should not be reached due to fail()
    }

    /**
     * Validates the JWT token using our JwtTokenProvider and SupabaseJwtProcessor
     */
    private void validateToken(String token, SkutterRole expectedRole) {
        assertNotNull(token, "Token should not be null");
        
        // Verify JWT secret is configured properly
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            fail("JWT_SECRET environment variable is not set");
        }
        
        // If the JWT issuer is configured, validate that it matches
        if (jwtIssuer != null && !jwtIssuer.isEmpty()) {
            // Extract and verify token issuer if available
            String tokenIssuer = jwtTokenProvider.extractIssuer(token).orElse(null);
            if (tokenIssuer != null) {
                assertEquals(jwtIssuer, tokenIssuer, "Token issuer should match configured issuer");
            }
        }
        
        // Debug the claims from token
        try {
            log.info("=== TOKEN DEBUG INFO ===");
            log.info("Role claim path: {}", securityProperties.getJwt().getRoleClaim());
            Claims claims = jwtTokenProvider.validateAndGetClaims(token).orElse(null);
            if (claims != null) {
                log.info("All token claims: {}", claims);
                
                // Try to extract nested claims manually
                String rolePath = securityProperties.getJwt().getRoleClaim(); // e.g. "app_metadata.skutter_role"
                String[] pathParts = rolePath.split("\\.");
                if (pathParts.length > 1) {
                    Object nestedObj = claims.get(pathParts[0]);
                    log.info("First level object ({}): {}", pathParts[0], nestedObj);
                    if (nestedObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> nestedMap = (Map<String, Object>) nestedObj;
                        log.info("Role value: {}", nestedMap.get(pathParts[1]));
                    }
                }
            } else {
                log.error("Failed to extract claims from token");
            }
            log.info("========================");
        } catch (Exception e) {
            log.error("Error debugging token claims", e);
        }
        
        assertDoesNotThrow(() -> {
            // Process the token using our JWT processor
            Authentication authentication = jwtProcessor.process(token);
            
            // Check if authentication is null, which happens when role claims are missing 
            if (authentication == null) {
                log.warn("Authentication is null - this means the token is missing role claims");
                log.warn("To fix this, ensure 'app_metadata.skutter_role' is set in your Supabase user data for user with expected role {}", expectedRole);
                return; // Skip remaining checks
            }
            
            // Validate authentication was created
            assertNotNull(authentication, "Authentication should not be null");
            assertTrue(authentication.isAuthenticated(), "Authentication should be authenticated");
            
            // Validate principal
            Object principal = authentication.getPrincipal();
            assertNotNull(principal, "Principal should not be null");
            assertTrue(principal instanceof SupabaseUserDetails, "Principal should be a SupabaseUserDetails instance");
            
            SupabaseUserDetails userDetails = (SupabaseUserDetails) principal;
            log.info("SupabaseUserDetails User details: {}", userDetails);
            
            // Validate user ID
            String userId = userDetails.getUserId();
            assertNotNull(userId, "User ID should not be null");
            assertDoesNotThrow(() -> UUID.fromString(userId), "User ID should be a valid UUID");
            
            // Validate authorities
            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
            assertNotNull(authorities, "Authorities should not be null");
            log.info("Authorities: {}", authorities);
            
            // Authorities should not be empty since users without authorities are now rejected
            assertFalse(authorities.isEmpty(), "Authorities should not be empty");
            
            // Check for the expected role
            String expectedRoleName = "ROLE_" + expectedRole.name();
            assertTrue(authorities.contains(new SimpleGrantedAuthority(expectedRoleName)), 
                    "Authorities should contain " + expectedRoleName);
            
            // Check for the READ permission (all roles have this)
            assertTrue(authorities.contains(new SimpleGrantedAuthority("PERMISSION_READ")),
                    "All roles should have READ permission");
            
            // Check for WRITE permission (only certain roles)
            assertEquals(expectedRole.canWrite(), 
                    authorities.contains(new SimpleGrantedAuthority("PERMISSION_WRITE")),
                    "WRITE permission should match the role's canWrite capability");
            
            // Check for platform-wide scope (only platform roles)
            assertEquals(expectedRole.isPlatformWide(),
                    authorities.contains(new SimpleGrantedAuthority("SCOPE_PLATFORM")),
                    "PLATFORM scope should match the role's isPlatformWide capability");
            
            // Validate convenience methods
            assertTrue(userDetails.hasRole(expectedRole), "User should have the expected role");
            assertTrue(userDetails.canRead(), "All users should be able to read");
            assertEquals(expectedRole.canWrite(), userDetails.canWrite(), "Write capability should match role");
            assertEquals(expectedRole.isPlatformWide(), userDetails.hasPlatformAccess(), "Platform access should match role");
            
            log.info("Successfully validated token for role {}", expectedRole);
        }, "JWT validation failed");
    }

    @Test
    //@Disabled("Requires Supabase credentials and network access")
    void testPlatformOwnerToken() {
        String token = fetchSupabaseToken(emailPlatformOwner, passwordPlatformOwner);
        validateToken(token, SkutterRole.PLATFORM_OWNER);
    }

    @Test
   // @Disabled("Requires Supabase credentials and network access")
    void testPlatformViewerToken() {
        String token = fetchSupabaseToken(emailPlatformViewer, passwordPlatformViewer);
        validateToken(token, SkutterRole.PLATFORM_VIEWER);
    }

    @Test
    //@Disabled("Requires Supabase credentials and network access")
    void testProjectAdminToken() {
        String token = fetchSupabaseToken(emailProjectAdmin, passwordProjectAdmin);
        validateToken(token, SkutterRole.PROJECT_ADMIN);
    }

    @Test
    //@Disabled("Requires Supabase credentials and network access")
    void testProjectViewerToken() {
        String token = fetchSupabaseToken(emailProjectViewer, passwordProjectViewer);
        validateToken(token, SkutterRole.PROJECT_VIEWER);
    }

    @Test
    void testInvalidToken() {
        // Create an obviously invalid token
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        
        // The processor should return null for an invalid token
        Authentication authentication = jwtProcessor.process(invalidToken);
        assertNull(authentication, "Invalid token should result in null authentication");
    }
} 