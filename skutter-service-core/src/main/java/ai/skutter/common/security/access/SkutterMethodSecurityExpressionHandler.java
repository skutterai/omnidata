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

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;

/**
 * Custom method security expression handler that provides the SkutterSecurityExpressionRoot
 */
public class SkutterMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {
    
    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
    
    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(Authentication authentication, MethodInvocation invocation) {
        SkutterSecurityExpressionRoot root = new SkutterSecurityExpressionRoot(authentication);
        root.setTrustResolver(trustResolver);
        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setRoleHierarchy(getRoleHierarchy());
        return root;
    }
} 