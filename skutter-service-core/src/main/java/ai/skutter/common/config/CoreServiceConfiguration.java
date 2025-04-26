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
package ai.skutter.common.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ai.skutter.common.api.resilience.CircuitBreakerConfiguration;

/**
 * Core configuration that handles potential bean conflicts
 * by making the correct implementations primary.
 */
@Configuration
public class CoreServiceConfiguration {
    
    /**
     * This bean helps Spring decide which circuit breaker configuration to use
     * when applications include the core module. By making this a @Primary bean,
     * we ensure that Resilience4jConfiguration is chosen when there are conflicts.
     */
    @Bean
    @Primary
    public Class<?> resilience4jResolutionHelper() {
        // This is a marker bean to make Resilience4jConfiguration primary
        return Resilience4jConfiguration.class;
    }
}