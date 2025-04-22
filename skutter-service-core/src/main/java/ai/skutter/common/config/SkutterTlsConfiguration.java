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

import ai.skutter.common.security.properties.SkutterSecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures TLS/SSL for the application
 */
@Configuration
@ConditionalOnProperty(prefix = "skutter.security.https", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SkutterTlsConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(SkutterTlsConfiguration.class);
    
    /**
     * Customizes the web server factory to enable TLS/SSL
     */
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> tlsCustomizer(
            SkutterSecurityProperties securityProperties) {
        
        SkutterSecurityProperties.Https httpsProperties = securityProperties.getHttps();
        
        return factory -> {
            log.info("Configuring TLS/SSL for the application");
            
            // Set HTTPS port
            factory.setPort(httpsProperties.getPort());
            
            // Configure SSL
            Ssl ssl = new Ssl();
            ssl.setEnabled(true);
            ssl.setKeyStore(httpsProperties.getKeystorePath());
            ssl.setKeyStorePassword(httpsProperties.getKeystorePassword());
            ssl.setKeyStoreType(httpsProperties.getKeystoreType());
            
            if (httpsProperties.getKeystoreAlias() != null && !httpsProperties.getKeystoreAlias().isEmpty()) {
                ssl.setKeyAlias(httpsProperties.getKeystoreAlias());
            }
            
            factory.setSsl(ssl);
            
            log.info("TLS/SSL configured successfully on port {}", httpsProperties.getPort());
        };
    }
} 