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

import ai.skutter.common.client.CorrelationIdClientInterceptor;
import ai.skutter.common.client.SecurityContextClientInterceptor;
import ai.skutter.common.observability.properties.SkutterObservabilityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@AutoConfiguration
@ConditionalOnClass(RestTemplate.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SkutterClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "skutter.observability.correlation", name = "propagate-to-downstream", havingValue = "true", matchIfMissing = true)
    public CorrelationIdClientInterceptor correlationIdClientInterceptor(SkutterObservabilityProperties observabilityProperties) {
        return new CorrelationIdClientInterceptor(observabilityProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "skutter.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SecurityContextClientInterceptor securityContextClientInterceptor() {
        return new SecurityContextClientInterceptor();
    }

    /**
     * Configuration to automatically add interceptors to RestTemplate beans.
     */
    @Configuration
    @Lazy // Ensures this runs after other RestTemplate configurations
    public static class RestTemplateCustomizerConfiguration {

        @Bean
        public RestTemplateCustomizer skutterRestTemplateCustomizer(List<ClientHttpRequestInterceptor> skutterInterceptors) {
            return restTemplate -> {
                // Add Skutter interceptors if they aren't already present
                List<ClientHttpRequestInterceptor> existingInterceptors = restTemplate.getInterceptors();
                for (ClientHttpRequestInterceptor interceptor : skutterInterceptors) {
                    if (!existingInterceptors.contains(interceptor)) {
                        existingInterceptors.add(interceptor);
                    }
                }
                restTemplate.setInterceptors(existingInterceptors);
            };
        }
        
        // Helper interface to allow autowiring the customizer
        public interface RestTemplateCustomizer {
            void customize(RestTemplate restTemplate);
        }
    }
    
    /**
     * Provides a RestTemplateBuilder bean that includes the Skutter interceptors.
     * Note: Applications should prefer injecting RestTemplateBuilder over RestTemplate directly.
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplateBuilder skutterRestTemplateBuilder(List<ClientHttpRequestInterceptor> skutterInterceptors) {
        return new RestTemplateBuilder()
                .additionalInterceptors(skutterInterceptors);
    }
} 