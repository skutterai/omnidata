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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Skutter Actuator endpoints.
 */
@ConfigurationProperties(prefix = "skutter.actuator")
public class SkutterActuatorProperties {

    /**
     * Whether to enable Actuator endpoints.
     */
    private boolean enabled = true;

    /**
     * Whether to require authentication for Actuator endpoints.
     */
    private boolean requireAuthentication = true;

    /**
     * Endpoints to expose by default.
     */
    private Endpoints endpoints = new Endpoints();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRequireAuthentication() {
        return requireAuthentication;
    }

    public void setRequireAuthentication(boolean requireAuthentication) {
        this.requireAuthentication = requireAuthentication;
    }

    public Endpoints getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Endpoints endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * Configuration for individual endpoint types.
     */
    public static class Endpoints {
        /**
         * Whether to expose health endpoints.
         */
        private boolean health = true;

        /**
         * Whether to expose info endpoints.
         */
        private boolean info = true;

        /**
         * Whether to expose metrics endpoints.
         */
        private boolean metrics = true;

        /**
         * Whether to expose logger management endpoints.
         */
        private boolean loggers = true;

        /**
         * Whether to expose environment endpoints.
         */
        private boolean env = false;

        /**
         * Whether to expose beans endpoints.
         */
        private boolean beans = false;

        /**
         * Whether to expose thread dump endpoints.
         */
        private boolean threaddump = false;

        /**
         * Whether to expose heap dump endpoints.
         */
        private boolean heapdump = false;

        public boolean isHealth() {
            return health;
        }

        public void setHealth(boolean health) {
            this.health = health;
        }

        public boolean isInfo() {
            return info;
        }

        public void setInfo(boolean info) {
            this.info = info;
        }

        public boolean isMetrics() {
            return metrics;
        }

        public void setMetrics(boolean metrics) {
            this.metrics = metrics;
        }

        public boolean isLoggers() {
            return loggers;
        }

        public void setLoggers(boolean loggers) {
            this.loggers = loggers;
        }

        public boolean isEnv() {
            return env;
        }

        public void setEnv(boolean env) {
            this.env = env;
        }

        public boolean isBeans() {
            return beans;
        }

        public void setBeans(boolean beans) {
            this.beans = beans;
        }

        public boolean isThreaddump() {
            return threaddump;
        }

        public void setThreaddump(boolean threaddump) {
            this.threaddump = threaddump;
        }

        public boolean isHeapdump() {
            return heapdump;
        }

        public void setHeapdump(boolean heapdump) {
            this.heapdump = heapdump;
        }
    }
} 