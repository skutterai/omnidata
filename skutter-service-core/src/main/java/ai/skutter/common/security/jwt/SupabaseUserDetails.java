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
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User details extracted from a Supabase JWT token
 */
@Getter
@ToString(exclude = "claims")
public class SupabaseUserDetails {

    private final String userId;
    private final String email;
    private final Claims claims;
    private final Collection<? extends GrantedAuthority> authorities;

    public SupabaseUserDetails(String userId, String email, Claims claims, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.claims = claims;
        this.authorities = authorities;
    }

    /**
     * Get raw claims from the JWT
     */
    public Claims getClaims() {
        return claims;
    }
    
    /**
     * Get a claim value by name
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getClaim(String name) {
        return Optional.ofNullable((T) claims.get(name));
    }
    
    /**
     * Get authorities, returns empty list if null
     */
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities != null ? authorities : Collections.emptyList();
    }
    
    /**
     * Get a nested claim value by path (e.g., "app_metadata.organization")
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getNestedClaim(String path) {
        String[] parts = path.split("\\.");
        Object current = claims;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
                if (current == null) {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        }
        
        return Optional.ofNullable((T) current);
    }
    
    /**
     * Check if user has the specified role
     */
    public boolean hasRole(String roleName) {
        return authorities.stream()
            .filter(a -> a instanceof SimpleGrantedAuthority)
            .map(a -> ((SimpleGrantedAuthority) a).getAuthority())
            .anyMatch(a -> a.equals("ROLE_" + roleName));
    }
    
    /**
     * Check if user has the specified role
     */
    public boolean hasRole(SkutterRole role) {
        return hasRole(role.name());
    }
    
    /**
     * Check if user has permission to read
     */
    public boolean canRead() {
        return authorities.stream()
            .filter(a -> a instanceof SimpleGrantedAuthority)
            .map(a -> ((SimpleGrantedAuthority) a).getAuthority())
            .anyMatch(a -> a.equals("PERMISSION_READ"));
    }
    
    /**
     * Check if user has permission to write
     */
    public boolean canWrite() {
        return authorities.stream()
            .filter(a -> a instanceof SimpleGrantedAuthority)
            .map(a -> ((SimpleGrantedAuthority) a).getAuthority())
            .anyMatch(a -> a.equals("PERMISSION_WRITE"));
    }
    
    /**
     * Check if user has platform-wide access
     */
    public boolean hasPlatformAccess() {
        return authorities.stream()
            .filter(a -> a instanceof SimpleGrantedAuthority)
            .map(a -> ((SimpleGrantedAuthority) a).getAuthority())
            .anyMatch(a -> a.equals("SCOPE_PLATFORM"));
    }
} 