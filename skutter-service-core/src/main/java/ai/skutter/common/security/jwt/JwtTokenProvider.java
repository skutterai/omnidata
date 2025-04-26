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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final SkutterSecurityProperties properties;
    private Key key;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        initializeKey();
    }

    public String generateToken(String subject) {
        log.debug("Generating JWT token for subject: {}", subject);
        String token = Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + properties.getJwt().getExpirationMs()))
                .signWith(key)
                .compact();
        log.trace("Generated JWT token with expiration: {}ms", properties.getJwt().getExpirationMs());
        return token;
    }

    public boolean validateToken(String token) {
        try {
            log.trace("Validating JWT token");
            jwtParser.parseSignedClaims(token);
            log.debug("JWT token validation successful");
            return true;
        } catch (Exception e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        log.trace("Extracting username from JWT token");
        String username = getClaimFromToken(token, Claims::getSubject);
        log.debug("Extracted username: {} from JWT token", username);
        return username;
    }

    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        log.trace("Getting claim from JWT token");
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        log.trace("Getting all claims from JWT token");
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    /**
     * Initialize the key used for JWT validation
     */
    private void initializeKey() {
        try {
            if (StringUtils.hasText(properties.getJwt().getPublicKeyPath())) {
                // Load public key from file
                this.key = loadPublicKey(properties.getJwt().getPublicKeyPath());
                log.info("Initialized JWT provider with public key from: {}", 
                         properties.getJwt().getPublicKeyPath());
            } else if (StringUtils.hasText(properties.getJwt().getSecret())) {
                // Use secret key
                this.key = Keys.hmacShaKeyFor(
                    properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
                log.info("Initialized JWT provider with provided secret key");
            } else {
                // Generate a random key for dev environments
                log.warn("No JWT secret or public key provided, generating a temporary one. " + 
                         "This is not secure for production use!");
                this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            }
            
            // Create JWT parser
            var parserBuilder = Jwts.parser();
            if (this.key instanceof SecretKey) {
                log.debug("Using HMAC secret key for JWT validation");
                parserBuilder = parserBuilder.verifyWith((SecretKey) this.key);
            } else if (this.key instanceof PublicKey) {
                log.debug("Using public key for JWT validation");
                parserBuilder = parserBuilder.verifyWith((PublicKey) this.key);
            } else {
                String error = "Unsupported key type: " + this.key.getClass().getName();
                log.error(error);
                throw new IllegalStateException(error);
            }
            
            this.jwtParser = parserBuilder.build();
            log.info("JWT parser initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize JWT token provider", e);
            throw new RuntimeException("Failed to initialize JWT token provider", e);
        }
    }

    /**
     * Load a public key from a file path
     */
    private PublicKey loadPublicKey(String publicKeyPath) throws Exception {
        log.debug("Loading public key from path: {}", publicKeyPath);
        File file = ResourceUtils.getFile(publicKeyPath);
        
        if (publicKeyPath.endsWith(".pem") || publicKeyPath.endsWith(".key")) {
            log.debug("Loading PEM format public key");
            String key = new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            String publicKeyPEM = key
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
            
            byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            return keyFactory.generatePublic(keySpec);
        } else if (publicKeyPath.endsWith(".crt") || publicKeyPath.endsWith(".cer")) {
            log.debug("Loading certificate format public key");
            try (FileInputStream fis = new FileInputStream(file)) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
                return cert.getPublicKey();
            }
        } else {
            String error = "Unsupported public key format: " + publicKeyPath;
            log.error(error);
            throw new IllegalArgumentException(error);
        }
    }

    /**
     * Update the JWT secret and reinitialize the provider
     * @param newSecret The new secret to use
     */
    public void updateSecret(String newSecret) {
        if (StringUtils.hasText(newSecret)) {
            log.info("Updating JWT provider with new secret key");
            this.key = Keys.hmacShaKeyFor(newSecret.getBytes(StandardCharsets.UTF_8));
            
            // Recreate JWT parser with new key
            this.jwtParser = Jwts.parser()
                .verifyWith((SecretKey) this.key)
                .build();
            
            log.info("JWT parser reinitialized with new secret");
        }
    }

    /**
     * Extract the issuer from a JWT token
     */
    public Optional<String> extractIssuer(String token) {
        try {
            if (!StringUtils.hasText(token)) {
                return Optional.empty();
            }
            
            log.trace("Extracting issuer from JWT token");
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            String issuer = claims.getIssuer();
            
            if (StringUtils.hasText(issuer)) {
                log.debug("Extracted issuer from token: {}", issuer);
                return Optional.of(issuer);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.debug("Failed to extract issuer from token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Validate a JWT token and return the claims if valid
     */
    public Optional<Claims> validateAndGetClaims(String token) {
        try {
            if (!StringUtils.hasText(token)) {
                log.debug("Empty token provided for validation");
                return Optional.empty();
            }
            
            log.trace("Validating and parsing JWT token");
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            
            // Validate expiration if enabled
            if (properties.getJwt().isValidateExpiration()) {
                Date expiration = claims.getExpiration();
                if (expiration == null || expiration.before(new Date())) {
                    log.debug("JWT token is expired. Expiration: {}, Current time: {}", 
                            expiration, new Date());
                    return Optional.empty();
                }
                
                // Check if token will expire in less than an hour
                Date oneHourFromNow = new Date(System.currentTimeMillis() + 3600000); // 1 hour in milliseconds
                if (expiration.before(oneHourFromNow)) {
                    log.warn("JWT token will expire soon. Token expires at: {}, current time: {}, minutes remaining: {}", 
                            expiration, new Date(), (expiration.getTime() - System.currentTimeMillis()) / 60000);
                }
                log.trace("JWT token expiration validated successfully");
            }
            
            log.debug("JWT token validation successful");
            return Optional.of(claims);
        } catch (Exception e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extract a nested claim value from a path like "app_metadata.skutter_role"
     */
    public Optional<Object> extractNestedClaim(Claims claims, String claimPath) {
        if (claims == null || !StringUtils.hasText(claimPath)) {
            log.debug("Invalid input for nested claim extraction. Claims: {}, Path: {}", 
                    claims, claimPath);
            return Optional.empty();
        }
        
        log.trace("Extracting nested claim: {}", claimPath);
        String[] pathParts = claimPath.split("\\.");
        Object currentValue = claims;
        
        for (String part : pathParts) {
            if (currentValue instanceof Claims) {
                currentValue = ((Claims) currentValue).get(part);
            } else if (currentValue instanceof java.util.Map) {
                currentValue = ((java.util.Map<?, ?>) currentValue).get(part);
            } else {
                log.debug("Invalid claim structure at path part: {}", part);
                return Optional.empty();
            }
            
            if (currentValue == null) {
                log.debug("Claim not found at path part: {}", part);
                return Optional.empty();
            }
        }
        
        log.trace("Successfully extracted nested claim: {} = {}", claimPath, currentValue);
        return Optional.ofNullable(currentValue);
    }
} 