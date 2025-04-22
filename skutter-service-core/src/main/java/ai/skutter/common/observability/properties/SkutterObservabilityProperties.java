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
package ai.skutter.common.observability.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
@ConfigurationProperties(prefix = "skutter.observability")
public class SkutterObservabilityProperties {

    /**
     * Correlation ID configuration
     */
    @NestedConfigurationProperty
    private CorrelationProperties correlation = new CorrelationProperties();
    
    /**
     * Metrics configuration.
     * @deprecated Use standard management.metrics.* properties instead.
     */
    @Deprecated
    @NestedConfigurationProperty
    private MetricsProperties metrics = new MetricsProperties();
    
    /**
     * Logging configuration
     */
    @NestedConfigurationProperty
    private LoggingProperties logging = new LoggingProperties();
    
    @Data
    public static class CorrelationProperties {
        /**
         * Header name for correlation ID
         */
        private String headerName = "X-Correlation-ID";
        
        /**
         * Generate correlation ID if missing
         */
        private boolean generateIfMissing = true;
        
        /**
         * Propagate correlation ID to downstream services
         */
        private boolean propagateToDownstream = true;
    }
    
    /**
     * @deprecated Use standard management.metrics.* properties instead.
     */
    @Data
    @Deprecated
    public static class MetricsProperties {
        /**
         * Enable or disable metrics collection
         */
        private boolean enabled = true;
        
        /**
         * Application name to use in metrics
         */
        private String applicationName;
        
        /**
         * Enable standardized unit naming
         */
        private boolean useStandardizedUnits = true;
        
        /**
         * Include JVM metrics
         */
        private boolean includeJvmMetrics = true;
        
        /**
         * Include system metrics
         */
        private boolean includeSystemMetrics = true;
    }
    
    @Data
    public static class LoggingProperties {
        /**
         * Minimum log level
         */
        private String level = "INFO";
        
        /**
         * Include correlation ID in logs
         */
        private boolean includeCorrelationId = true;
        
        /**
         * Include user ID in logs
         */
        private boolean includeUserId = true;
        
        /**
         * Include request details in logs
         */
        private boolean includeRequestDetails = true;
    }
} 