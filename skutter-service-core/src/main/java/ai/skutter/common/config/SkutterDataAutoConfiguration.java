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

import ai.skutter.common.data.auditing.AuditingConfig;
import ai.skutter.common.data.jdbc.PostgresSetUserIdInterceptor;
import ai.skutter.common.data.properties.SkutterDataProperties;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@AutoConfiguration
@EnableConfigurationProperties(SkutterDataProperties.class)
@ComponentScan("ai.skutter.common.data")
@Import(AuditingConfig.class)
public class SkutterDataAutoConfiguration {

    private final SkutterDataProperties dataProperties;

    public SkutterDataAutoConfiguration(SkutterDataProperties dataProperties) {
        this.dataProperties = dataProperties;
    }

    @Bean
    @ConditionalOnProperty(prefix = "skutter.data", name = "enable-user-id-propagation", havingValue = "true")
    public PostgresSetUserIdInterceptor postgresSetUserIdInterceptor(JdbcTemplate jdbcTemplate) {
        return new PostgresSetUserIdInterceptor(jdbcTemplate);
    }
    
    /**
     * Configure Flyway with custom settings if needed
     */
    @Bean
    @ConditionalOnClass(Flyway.class)
    @ConditionalOnProperty(prefix = "skutter.data.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(Flyway.class)
    public Flyway flyway(DataSource dataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations(dataProperties.getFlyway().getLocations())
            .baselineOnMigrate(dataProperties.getFlyway().isBaselineOnMigrate())
            .validateOnMigrate(dataProperties.getFlyway().isValidateOnMigrate())
            .load()
            .migrate();
        return Flyway.configure()
            .dataSource(dataSource)
            .load();
    }
} 