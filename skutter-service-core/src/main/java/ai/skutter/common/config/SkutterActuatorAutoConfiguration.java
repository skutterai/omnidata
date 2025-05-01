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

import ai.skutter.common.observability.logging.LoggingController;
import ai.skutter.common.observability.properties.SkutterActuatorProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import ai.skutter.common.security.role.SkutterRole;

/**
 * Auto-configuration for Skutter Actuator endpoints.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
@EnableConfigurationProperties({SkutterActuatorProperties.class, WebEndpointProperties.class})
@ConditionalOnProperty(prefix = "skutter.actuator", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SkutterActuatorAutoConfiguration {

    /**
     * Creates the logging controller that provides both REST API and Actuator endpoint for managing loggers.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "skutter.actuator.endpoints", name = "loggers", havingValue = "true", matchIfMissing = true)
    public LoggingController loggingController(LoggingSystem loggingSystem) {
        return new LoggingController(loggingSystem);
    }

    /**
     * Configuration for securing Actuator endpoints.
     */
    @Configuration
    @ConditionalOnClass(name = {"org.springframework.security.config.annotation.web.builders.HttpSecurity"})
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class ActuatorSecurityConfiguration {

        private final SkutterActuatorProperties properties;
        private final WebEndpointProperties webEndpointProperties;

        public ActuatorSecurityConfiguration(
                SkutterActuatorProperties properties,
                WebEndpointProperties webEndpointProperties) {
            this.properties = properties;
            this.webEndpointProperties = webEndpointProperties;
        }

        /**
         * Configures security for Actuator endpoints.
         */
        @Bean
        @ConditionalOnMissingBean(name = "actuatorSecurityFilterChain")
        public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
            // Base path for actuator endpoints
            String basePath = webEndpointProperties.getBasePath();
            String platformOwnerRole = SkutterRole.PLATFORM_OWNER.name();
            String platformViewerRole = SkutterRole.PLATFORM_VIEWER.name();

            http.securityMatcher(basePath + "/**")
                .authorizeHttpRequests(authorize -> {
                    // Public endpoints - health and info are always public if enabled
                    if (properties.getEndpoints().isHealth()) {
                        authorize.requestMatchers(basePath + "/health/**").permitAll();
                    }
                    if (properties.getEndpoints().isInfo()) {
                        authorize.requestMatchers(basePath + "/info").permitAll();
                    }

                    // Authenticated endpoints
                    if (properties.isRequireAuthentication()) {

                        // Allow GET access for OWNER and VIEWER to common read-only endpoints
                        authorize.requestMatchers(HttpMethod.GET,
                            basePath + "/loggers/**",
                            basePath + "/skutter-loggers/**",
                            basePath + "/metrics/**",
                            basePath + "/beans",
                            basePath + "/threaddump"
                        ).hasAnyRole(platformOwnerRole, platformViewerRole);

                        // Allow OWNER only for other methods on loggers (e.g., POST)
                        authorize.requestMatchers(HttpMethod.POST, basePath + "/loggers/**").hasRole(platformOwnerRole);
                        authorize.requestMatchers(HttpMethod.POST, basePath + "/skutter-loggers/**").hasRole(platformOwnerRole);

                        // Allow OWNER only for potentially sensitive endpoints (all methods)
                        if (properties.getEndpoints().isEnv()) {
                            authorize.requestMatchers(basePath + "/env/**").hasRole(platformOwnerRole);
                        }
                        if (properties.getEndpoints().isHeapdump()) {
                            authorize.requestMatchers(basePath + "/heapdump/**").hasRole(platformOwnerRole);
                        }
                        
                        // Default for any other actuator request: Require PLATFORM_OWNER
                        authorize.anyRequest().hasRole(platformOwnerRole); 
                        
                    } else {
                        // If authentication is not required by config, permit all actuator endpoints
                        authorize.anyRequest().permitAll();
                    }
                });

            // Ensure stateless session management for actuator endpoints
            http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

            // Disable CSRF for Actuator endpoints as they are typically not called from browsers
            // and we use JWT for stateless auth. Re-enable if needed for specific use cases.
            http.csrf(csrf -> csrf.disable()); 

            return http.build();
        }
    }
} 