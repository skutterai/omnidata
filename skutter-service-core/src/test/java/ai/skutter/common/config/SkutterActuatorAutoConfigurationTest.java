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
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SkutterActuatorAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SkutterActuatorAutoConfiguration.class,
                    WebMvcAutoConfiguration.class, // Needed for WebEndpointProperties, MVC context
                    SecurityAutoConfiguration.class, // Needed for HttpSecurity bean
                    SecurityFilterAutoConfiguration.class, // Needed for SecurityFilterChain registration
                    EndpointAutoConfiguration.class, // Needed for ParameterValueMapper
                    WebEndpointAutoConfiguration.class // Needed for WebEndpointProperties
            ))
            .withUserConfiguration(TestLoggingConfiguration.class) // Provide a mock LoggingSystem
            .withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO)); // Optional: See conditional evaluation

    @Test
    void shouldLoadDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SkutterActuatorProperties.class);
            assertThat(context).hasSingleBean(LoggingController.class);
            assertThat(context).hasSingleBean(SecurityFilterChain.class); // Check for actuator filter chain
            
            SkutterActuatorProperties properties = context.getBean(SkutterActuatorProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.isRequireAuthentication()).isTrue();
            assertThat(properties.getEndpoints().isHealth()).isTrue();
            assertThat(properties.getEndpoints().isInfo()).isTrue();
            assertThat(properties.getEndpoints().isLoggers()).isTrue();
        });
    }

    @Test
    void shouldDisableActuatorWhenPropertyIsSet() {
        contextRunner
                .withPropertyValues("skutter.actuator.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LoggingController.class);
                    // Check that OUR specific filter chain is not created when actuator is disabled
                    assertThat(context).doesNotHaveBean("actuatorSecurityFilterChain"); 
                });
    }

    @Test
    void shouldDisableLoggersEndpointWhenPropertyIsSet() {
        contextRunner
                .withPropertyValues("skutter.actuator.endpoints.loggers=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LoggingController.class);
                    assertThat(context).hasSingleBean(SecurityFilterChain.class); // Filter chain should still exist if actuator is enabled
                });
    }

    @Test
    void shouldConfigureSecurityWithAuthenticationRequired() {
        contextRunner
                .withPropertyValues("skutter.actuator.require-authentication=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                    SkutterActuatorProperties properties = context.getBean(SkutterActuatorProperties.class);
                    assertThat(properties.isRequireAuthentication()).isTrue();
                    // Further tests could use MockMvc to verify endpoint access rules
                });
    }

    @Test
    void shouldConfigureSecurityWithoutAuthenticationRequired() {
        contextRunner
                .withPropertyValues("skutter.actuator.require-authentication=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                    SkutterActuatorProperties properties = context.getBean(SkutterActuatorProperties.class);
                    assertThat(properties.isRequireAuthentication()).isFalse();
                    // Further tests could use MockMvc to verify endpoint access rules (e.g., permitAll)
                });
    }

    // Add a mock LoggingSystem bean for the test context
    @Configuration
    static class TestLoggingConfiguration {
        @Bean
        LoggingSystem loggingSystem() {
            return mock(LoggingSystem.class);
        }
    }
} 