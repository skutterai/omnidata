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
package ai.skutter.common.data.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "skutter.data")
public class SkutterDataProperties {

    /**
     * Enable or disable user ID propagation to Postgres session variables (for RLS).
     */
    private boolean enableUserIdPropagation = true;

    /**
     * Flyway migration configuration.
     * @deprecated Use standard spring.flyway.* properties instead.
     */
    @Deprecated
    private final Flyway flyway = new Flyway();

    /**
     * PostGIS configuration
     */
    private final PostGis postGis = new PostGis();
    
    /**
     * @deprecated Use standard spring.flyway.* properties instead.
     */
    @Data
    @Deprecated
    public static class Flyway {
        /**
         * Enable or disable Flyway migrations
         */
        private boolean enabled = true;
        
        /**
         * Locations to scan for migrations
         */
        private String[] locations = {"classpath:db/migration"};
        
        /**
         * Whether to baseline an existing database
         */
        private boolean baselineOnMigrate = false;
        
        /**
         * Whether to validate migrations on startup
         */
        private boolean validateOnMigrate = true;
    }
    
    @Data
    public static class PostGis {
        /**
         * Enable or disable PostGIS support
         */
        private boolean enabled = true;
        
        /**
         * Default SRID for spatial data
         */
        private int defaultSrid = 4326;
    }
}