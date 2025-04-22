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

package ai.skutter.common.security.access;

import ai.skutter.common.security.jwt.SupabaseUserDetails;
import ai.skutter.common.security.role.SkutterRole;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Custom security expression root that provides platform-wide access control
 */
public class SkutterSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private Object filterObject;
    private Object returnObject;
    private Object target;

    public SkutterSecurityExpressionRoot(Authentication authentication) {
        super(authentication);
    }

    /**
     * Check if the user has the PLATFORM_OWNER role
     */
    public boolean isPlatformOwner() {
        return hasRole(SkutterRole.PLATFORM_OWNER.name());
    }

    /**
     * Check if the user has the PLATFORM_VIEWER role
     */
    public boolean isPlatformViewer() {
        return hasRole(SkutterRole.PLATFORM_VIEWER.name());
    }

    /**
     * Check if the user has the PROJECT_ADMIN role
     */
    public boolean isProjectAdmin() {
        return hasRole(SkutterRole.PROJECT_ADMIN.name());
    }

    /**
     * Check if the user has the PROJECT_VIEWER role
     */
    public boolean isProjectViewer() {
        return hasRole(SkutterRole.PROJECT_VIEWER.name());
    }

    /**
     * Check if the user has platform-wide access
     */
    public boolean hasPlatformAccess() {
        // Check for platform-specific scope
        boolean hasPlatformScope = getAuthentication().getAuthorities().stream()
                .filter(a -> a instanceof SimpleGrantedAuthority)
                .map(a -> ((SimpleGrantedAuthority) a).getAuthority())
                .anyMatch(a -> a.equals("SCOPE_PLATFORM"));
                
        return hasPlatformScope || isPlatformOwner() || isPlatformViewer();
    }

    /**
     * Check if the user has write access to the platform
     */
    public boolean hasPlatformWriteAccess() {
        return isPlatformOwner();
    }

    /**
     * Check if the user has read access to the platform
     */
    public boolean hasPlatformReadAccess() {
        return isPlatformOwner() || isPlatformViewer();
    }

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public Object getThis() {
        return target;
    }
    
    public void setThis(Object target) {
        this.target = target;
    }
} 