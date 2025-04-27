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

import ai.skutter.common.security.access.SkutterMethodSecurityExpressionHandler;
import ai.skutter.common.security.jwt.JwtAuthenticationFilter;
import ai.skutter.common.security.jwt.JwtTokenProvider;
import ai.skutter.common.security.jwt.SupabaseJwtProcessor;
import ai.skutter.common.security.logging.SecurityLoggingFilter;
import ai.skutter.common.security.properties.SkutterSecurityProperties;
import ai.skutter.common.security.entrypoint.CustomBearerAuthenticationEntryPoint;
import ai.skutter.common.security.handler.CustomBearerAccessDeniedHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.Arrays;

@AutoConfiguration
@EnableConfigurationProperties(SkutterSecurityProperties.class)
@ComponentScan("ai.skutter.common.security")
public class SkutterSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenProvider jwtTokenProvider(SkutterSecurityProperties securityProperties) {
        return new JwtTokenProvider(securityProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SupabaseJwtProcessor supabaseJwtProcessor(JwtTokenProvider jwtTokenProvider) {
        return new SupabaseJwtProcessor(jwtTokenProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            SupabaseJwtProcessor supabaseJwtProcessor,
            SkutterSecurityProperties securityProperties) {
        return new JwtAuthenticationFilter(jwtTokenProvider, supabaseJwtProcessor, securityProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityLoggingFilter securityLoggingFilter() {
        return new SecurityLoggingFilter();
    }
    
    /**
     * Custom method security expression handler for Skutter roles and project access
     */
    @Bean
    @ConditionalOnMissingBean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        return new SkutterMethodSecurityExpressionHandler();
    }

    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity
    @ConditionalOnProperty(prefix = "skutter.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    public static class SecurityConfiguration {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final SecurityLoggingFilter securityLoggingFilter;
        private final SkutterSecurityProperties securityProperties;
        private final AuthenticationEntryPoint customAuthenticationEntryPoint;
        private final AccessDeniedHandler customAccessDeniedHandler;

        public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter,
                                    SecurityLoggingFilter securityLoggingFilter,
                                    SkutterSecurityProperties securityProperties,
                                    CustomBearerAuthenticationEntryPoint customAuthenticationEntryPoint,
                                    CustomBearerAccessDeniedHandler customAccessDeniedHandler) {
            this.jwtAuthenticationFilter = jwtAuthenticationFilter;
            this.securityLoggingFilter = securityLoggingFilter;
            this.securityProperties = securityProperties;
            this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
            this.customAccessDeniedHandler = customAccessDeniedHandler;
        }
        
        /**
         * Configure CORS for the application
         */
        @Bean
        @ConditionalOnMissingBean
        public CorsConfigurationSource corsConfigurationSource() {
            SkutterSecurityProperties.Cors corsProperties = securityProperties.getCors();
            
            if (!corsProperties.isEnabled()) {
                return request -> null; // Return null if CORS is disabled
            }
            
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
            configuration.setAllowedMethods(corsProperties.getAllowedMethods());
            configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
            configuration.setAllowCredentials(corsProperties.isAllowCredentials());
            configuration.setMaxAge(corsProperties.getMaxAge());
            
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            // Disable CSRF as we're using stateless JWT authentication
            http.csrf(AbstractHttpConfigurer::disable);
            
            // Enable CORS
            if (securityProperties.getCors().isEnabled()) {
                http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
            }
            
            // Configure HTTPS if enabled
            if (securityProperties.getHttps().isEnabled()) {
                // Require secure channel (HTTPS) for all requests
                http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
                
                // Configure HSTS (HTTP Strict Transport Security) if enabled
                if (securityProperties.getHttps().isHstsEnabled()) {
                    http.headers(headers -> 
                        headers.httpStrictTransportSecurity(hsts -> 
                            hsts
                                .includeSubDomains(securityProperties.getHttps().isHstsIncludeSubDomains())
                                .maxAgeInSeconds(securityProperties.getHttps().getHstsMaxAgeSeconds())
                                .preload(securityProperties.getHttps().isHstsPreload())
                        )
                    );
                }
            }
            
            // Configure stateless session management
            http.sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

            // Configure path-based security
            http.authorizeHttpRequests(auth -> {
                // Public endpoints
                securityProperties.getPublicPaths().forEach(path -> 
                    auth.requestMatchers(path).permitAll()
                );
                
                // Secured endpoints
                auth.anyRequest().authenticated();
            });

            // Configure custom exception handling
            http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(customAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
            );

            // Add JWT filter before UsernamePasswordAuthenticationFilter
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
            // Add logging filter
            http.addFilterAfter(securityLoggingFilter, JwtAuthenticationFilter.class);

            return http.build();
        }
    }
} 