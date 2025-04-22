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

package ai.skutter.common.security.role;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Enumeration of roles used in the Skutter platform
 */
public enum SkutterRole {
    /**
     * Platform owner with full read/write access to all APIs and projects
     */
    PLATFORM_OWNER(true, true),
    
    /**
     * Platform viewer with read access to all APIs and projects
     */
    PLATFORM_VIEWER(true, false),
    
    /**
     * Project admin with read/write access to specific projects they're assigned to
     */
    PROJECT_ADMIN(false, true),
    
    /**
     * Project viewer with read access to specific projects they're assigned to
     */
    PROJECT_VIEWER(false, false);
    
    private final boolean platformWide;
    private final boolean canWrite;
    
    SkutterRole(boolean platformWide, boolean canWrite) {
        this.platformWide = platformWide;
        this.canWrite = canWrite;
    }
    
    /**
     * Whether this role has platform-wide access
     */
    public boolean isPlatformWide() {
        return platformWide;
    }
    
    /**
     * Whether this role can write (modify data)
     */
    public boolean canWrite() {
        return canWrite;
    }
    
    /**
     * Check if this role can perform an operation based on read/write access
     * @param requiresWrite whether the operation requires write access
     * @return true if the role can perform the operation
     */
    public boolean canPerform(boolean requiresWrite) {
        return !requiresWrite || canWrite;
    }
    
    /**
     * Get all roles that have platform-wide access
     */
    public static Set<SkutterRole> getPlatformWideRoles() {
        return new HashSet<>(Arrays.asList(PLATFORM_OWNER, PLATFORM_VIEWER));
    }
    
    /**
     * Get all roles that have write access
     */
    public static Set<SkutterRole> getWriteAccessRoles() {
        return new HashSet<>(Arrays.asList(PLATFORM_OWNER, PROJECT_ADMIN));
    }
} 