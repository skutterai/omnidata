/**
 * Integration test to verify Flyway migrations against a live PostgreSQL database
 * managed by Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
// Remove the problematic @TestPropertySource
@TestPropertySource(properties = {
    "spring.test.mockmvc.print=default",  // Replace with a harmless property
    "spring.flyway.validateMigrationNaming=true"
})
@Import({
    DataSourceAutoConfiguration.class,
    FlywayAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class
})
@org.springframework.test.context.ActiveProfiles("test")
public class FlywayIntegrationTest {
    // ... rest of class remains the same
} 