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

import ai.skutter.common.api.exception.GlobalExceptionHandler;
import ai.skutter.common.api.properties.SkutterApiProperties;
import ai.skutter.common.api.ratelimit.RateLimitInterceptor;
import ai.skutter.common.api.resilience.CircuitBreakerConfiguration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Lazy;

@AutoConfiguration
@EnableConfigurationProperties(SkutterApiProperties.class)
@ComponentScan("ai.skutter.common.api")
@Import(CircuitBreakerConfiguration.class)
public class SkutterApiAutoConfiguration implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SkutterApiAutoConfiguration.class);

    private RateLimitInterceptor rateLimitInterceptor;
    
    private final SkutterApiProperties apiProperties;

    public SkutterApiAutoConfiguration(SkutterApiProperties apiProperties) {
        this.apiProperties = apiProperties;
    }

    @Autowired
    public void setRateLimitInterceptor(@Lazy RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Evaluating conditions for RateLimitInterceptor registration...");
        boolean isBeanPresent = (rateLimitInterceptor != null);
        boolean isEnabledProperty = apiProperties.getRateLimit().isEnabled();
        log.info("RateLimitInterceptor bean present: {}", isBeanPresent);
        log.info("skutter.api.rate-limit.enabled property value: {}", isEnabledProperty);

        if (isBeanPresent && isEnabledProperty) {
            log.info("Registering RateLimitInterceptor for path /** with order 0");
            registry.addInterceptor(rateLimitInterceptor)
                    .addPathPatterns("/**")
                    .order(0);
        } else {
             log.warn("RateLimitInterceptor NOT registered. Bean present: {}, Enabled property: {}", isBeanPresent, isEnabledProperty);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitInterceptor rateLimitInterceptor(SkutterApiProperties apiProperties) {
        return new RateLimitInterceptor(
            apiProperties.getRateLimit().getLimit(), 
            apiProperties.getRateLimit().getRefreshPeriod()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "skutter.api.documentation", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OpenAPI customOpenAPI(SkutterApiProperties apiProperties) {
        return new OpenAPI()
            .info(new Info()
                .title(apiProperties.getDocumentation().getTitle())
                .version(apiProperties.getDocumentation().getVersion())
                .description(apiProperties.getDocumentation().getDescription())
                .contact(new Contact()
                    .name(apiProperties.getDocumentation().getContactName())
                    .email(apiProperties.getDocumentation().getContactEmail())
                    .url(apiProperties.getDocumentation().getContactUrl()))
                .license(new License()
                    .name(apiProperties.getDocumentation().getLicenseName())
                    .url(apiProperties.getDocumentation().getLicenseUrl())))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .in(SecurityScheme.In.HEADER)
                    .name("Authorization")))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
} 