/*
 * Copyright (c) 2024 Skutter.ai
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

package ai.skutter.common.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify Flyway migrations against a live PostgreSQL database
 * managed by Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({
    DataSourceAutoConfiguration.class,
    FlywayAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class
})
@org.springframework.test.context.ActiveProfiles("test")
// Explicitly configure listeners to exclude Mockito listener
@TestExecutionListeners(listeners = {
    ServletTestExecutionListener.class,
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class
    // Exclude ResetMocksTestExecutionListener
})
public class FlywayIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(FlywayIntegrationTest.class);

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    void flywayMigrations_shouldApplySuccessfully() {
        log.info("Verifying Flyway migrations status...");
        // 1. Verify Flyway bean is present (context loaded)
        assertNotNull(flyway, "Flyway bean should be present in the context.");
        assertNotNull(dataSource, "DataSource bean should be present in the context.");

        // 2. Get migration info from Flyway
        MigrationInfoService infoService = flyway.info();
        assertNotNull(infoService, "Flyway MigrationInfoService should be available.");

        MigrationInfo[] allMigrations = infoService.all();
        MigrationInfo[] appliedMigrations = infoService.applied();

        log.info("Found {} total migrations.", allMigrations.length);
        log.info("Found {} applied migrations.", appliedMigrations.length);

        // 3. Check if any migrations exist and were applied
        // Adjust this assertion based on whether migrations are expected
        if (allMigrations.length == 0) {
            log.warn("No Flyway migrations found in default location (classpath:db/migration). Test checks basic Flyway setup only.");
            // If no migrations are expected, this test still verifies Flyway initializes correctly.
        } else {
            assertTrue(appliedMigrations.length > 0, "At least one Flyway migration should have been applied if migrations exist.");
        }

        // 4. Verify the state of all applied migrations
        Arrays.stream(appliedMigrations).forEach(info -> {
            log.info("Checking applied migration: {} - State: {} - Description: {}",
                     info.getScript(), info.getState(), info.getDescription());
            assertEquals(MigrationState.SUCCESS, info.getState(),
                         "Migration " + info.getScript() + " should have state SUCCESS");
        });

        // 5. Optional: Verify schema existence (Example: Check for flyway_schema_history table)
        log.info("Performing basic schema check for 'flyway_schema_history' table...");
        try (Connection connection = dataSource.getConnection();
             ResultSet tables = connection.getMetaData().getTables(null, null, "flyway_schema_history", null)) {
            assertTrue(tables.next(), "'flyway_schema_history' table should exist after migrations");
            log.info("'flyway_schema_history' table found.");
            // Add more checks here for specific tables/columns created by your migrations
        } catch (SQLException e) {
            fail("Error checking database metadata for Flyway history table", e);
        }
        log.info("Flyway integration test completed successfully.");
    }
} 