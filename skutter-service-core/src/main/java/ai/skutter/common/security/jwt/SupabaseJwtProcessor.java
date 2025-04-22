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

import ai.skutter.common.security.role.SkutterRole;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import org.springframework.security.core.GrantedAuthority;

/**
 * Processor for Supabase JWT tokens that extracts authentication details
 */
@Slf4j
@RequiredArgsConstructor
public class SupabaseJwtProcessor {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Process a JWT token and create an Authentication object
     */
    public Authentication process(String token) {
        return jwtTokenProvider.validateAndGetClaims(token)
            .flatMap(this::createAuthentication)
            .orElse(null);
    }

    /**
     * Create an Authentication object from JWT claims
     */
    private Optional<Authentication> createAuthentication(Claims claims) {
        try {
            String userId = claims.getSubject();
            if (!StringUtils.hasText(userId)) {
                log.debug("JWT subject is empty");
                return Optional.empty();
            }

            // Get user role from the nested claim structure
            List<SimpleGrantedAuthority> authorities = getAuthorities(claims);
            
            // Reject users without any roles/authorities
            if (authorities.isEmpty()) {
                log.warn("User {} has no roles/authorities assigned. Authentication rejected.", userId);
                return Optional.empty();
            }
            
            // Create user principal with the authorities
            SupabaseUserDetails userDetails = createUserDetails(claims, authorities);
            
            // Create authentication token with authorities
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

            return Optional.of(authentication);
        } catch (Exception e) {
            log.debug("Error processing JWT claims", e);
            return Optional.empty();
        }
    }

    /**
     * Extract authorities from JWT claims
     */
    private List<SimpleGrantedAuthority> getAuthorities(Claims claims) {
        Optional<Object> roleClaim = jwtTokenProvider.extractNestedClaim(claims, "app_metadata.skutter_role");
        
        if (roleClaim.isEmpty()) {
            log.warn("No 'app_metadata.skutter_role' claim found in token for user: {}", claims.getSubject());
            return Collections.emptyList();
        }
        
        Object roles = roleClaim.get();
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        if (roles instanceof List) {
            // Handle multiple roles
            for (Object role : (List<?>) roles) {
                addAuthorityForRole(authorities, role.toString());
            }
        } else if (roles instanceof String) {
            // Handle single role
            addAuthorityForRole(authorities, (String) roles);
        } else {
            log.warn("Unexpected role format in token: {}", roles.getClass().getName());
        }
        
        if (authorities.isEmpty()) {
            log.warn("No authorities could be extracted from role claim for user: {}", claims.getSubject());
        }
        
        return authorities;
    }
    
    /**
     * Add the appropriate authority for a role
     */
    private void addAuthorityForRole(List<SimpleGrantedAuthority> authorities, String roleName) {
        // Add the role with ROLE_ prefix (Spring Security convention)
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
        
        // Add additional permissions based on role capabilities
        try {
            SkutterRole role = SkutterRole.valueOf(roleName);
            
            // Add READ permission for all roles
            authorities.add(new SimpleGrantedAuthority("PERMISSION_READ"));
            
            // Add WRITE permission for roles that can write
            if (role.canWrite()) {
                authorities.add(new SimpleGrantedAuthority("PERMISSION_WRITE"));
            }
            
            // Add platform-wide indicator for platform roles
            if (role.isPlatformWide()) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_PLATFORM"));
            }
        } catch (IllegalArgumentException e) {
            log.warn("Unknown role found in token: {}", roleName);
        }
    }

    /**
     * Create user details from JWT claims
     */
    private SupabaseUserDetails createUserDetails(Claims claims, Collection<? extends GrantedAuthority> authorities) {
        String userId = claims.getSubject();
            
        // Extract email if available
        String email = Optional.ofNullable(claims.get("email"))
            .map(Object::toString)
            .orElse(null);
            
        return new SupabaseUserDetails(userId, email, claims, authorities);
    }
} 