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

import ai.skutter.common.api.properties.SkutterApiProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4jConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "skutter.api.resilience", name = "circuit-breaker-enabled", havingValue = "true", matchIfMissing = true)
    public CircuitBreakerRegistry circuitBreakerRegistry(SkutterApiProperties apiProperties) {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(apiProperties.getResilience().getFailureThreshold())
            .slowCallRateThreshold(50)
            .slowCallDurationThreshold(Duration.ofSeconds(1))
            .permittedNumberOfCallsInHalfOpenState(10)
            .slidingWindowSize(100)
            .minimumNumberOfCalls(10)
            .waitDurationInOpenState(apiProperties.getResilience().getWaitDurationInOpenState())
            .build();
        
        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    @Bean
    @ConditionalOnProperty(prefix = "skutter.api.resilience", name = "circuit-breaker-enabled", havingValue = "true", matchIfMissing = true)
    public CircuitBreaker defaultCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker("default");
    }
} 