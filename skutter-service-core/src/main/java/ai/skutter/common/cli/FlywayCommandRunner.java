package ai.skutter.common.cli;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Executes Flyway commands via the command line.
 * Detects arguments like "flyway migrate" and runs the corresponding Flyway action
 * using the application's configured Flyway bean.
 *
 * Implements ExitCodeGenerator to provide a clean exit code after execution.
 * Runs with high precedence to execute before the main application might fully start.
 * Only active when a Flyway bean is configured.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Run before other CommandLineRunners
@ConditionalOnBean(Flyway.class) // Only activate if Flyway is configured
@Profile("!test") // Typically disable during unit/integration tests unless specifically needed
public class FlywayCommandRunner implements CommandLineRunner, ExitCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(FlywayCommandRunner.class);
    private static final String FLYWAY_ARG_PREFIX = "flyway";
    private static final List<String> SUPPORTED_COMMANDS = List.of("migrate", "info", "validate", "clean"); // Add more as needed

    private final Flyway flyway;
    private int exitCode = 0; // Default to success (0), runner did nothing or succeeded
    private boolean commandExecuted = false;

    @Autowired // Use constructor injection
    public FlywayCommandRunner(Flyway flyway) {
        // Ensure Flyway bean is provided
        this.flyway = Objects.requireNonNull(flyway, "Flyway bean cannot be null for FlywayCommandRunner");
    }

    @Override
    public void run(String... args) {
        if (args == null || args.length < 2 || !FLYWAY_ARG_PREFIX.equalsIgnoreCase(args[0])) {
            log.trace("No 'flyway <command>' arguments detected. FlywayCommandRunner doing nothing.");
            // No relevant args, let the application start normally (if it's meant to)
            return;
        }

        String command = args[1].toLowerCase();
        log.info("Flyway CLI command detected: {}", command);
        commandExecuted = true; // Mark that we are handling a CLI command

        if (!SUPPORTED_COMMANDS.contains(command)) {
            log.error("Unsupported Flyway command: '{}'. Supported commands are: {}", command, SUPPORTED_COMMANDS);
            this.exitCode = 2; // Indicate invalid argument
            return;
        }

        try {
            log.info("Executing Flyway command: {}", command);
            switch (command) {
                case "migrate":
                    executeMigrate();
                    break;
                case "info":
                    executeInfo();
                    break;
                case "validate":
                    executeValidate();
                    break;
                case "clean":
                    executeClean();
                    break;
                // Add cases for other Flyway commands here (e.g., baseline, repair)
                default:
                    // Should be caught by validation above, but good practice
                    log.error("Unhandled supported command: {}", command);
                    this.exitCode = 3;
                    break;
            }
            log.info("Flyway command '{}' executed successfully.", command);
            this.exitCode = 0; // Explicitly set success
        } catch (FlywayException e) {
            log.error("Flyway command '{}' failed: {}", command, e.getMessage(), e);
            this.exitCode = 1; // General failure exit code
        } catch (Exception e) {
            log.error("An unexpected error occurred during Flyway command '{}': {}", command, e.getMessage(), e);
            this.exitCode = 1; // General failure exit code
        }
    }

    private void executeMigrate() {
        log.info("Starting Flyway migration...");
        flyway.migrate();
        log.info("Flyway migration finished.");
    }

    private void executeInfo() {
        log.info("Retrieving Flyway migration info...");
        MigrationInfoService infoService = flyway.info();
        if (infoService == null) {
            log.warn("Flyway info service is null. Cannot display migration info.");
            return;
        }

        log.info("-----------------------------------------------------");
        log.info("Flyway Migration Info");
        log.info("-----------------------------------------------------");
        MigrationInfo current = infoService.current();
        if (current == null) {
            log.info("Schema History: <Empty>");
        } else {
            log.info("Current Version: {} (Description: {}, State: {})",
                     current.getVersion(), current.getDescription(), current.getState());
        }

        log.info("Applied Migrations:");
        if (infoService.applied().length == 0) {
            log.info("  <None>");
        } else {
            Arrays.stream(infoService.applied())
                  .forEach(m -> log.info("  + {} (Description: {}, Type: {}, State: {})",
                                         m.getVersion(), m.getDescription(), m.getType(), m.getState()));
        }

        log.info("Pending Migrations:");
        if (infoService.pending().length == 0) {
            log.info("  <None>");
        } else {
            Arrays.stream(infoService.pending())
                  .forEach(m -> log.info("  - {} (Description: {}, Type: {})",
                                         m.getVersion(), m.getDescription(), m.getType()));
        }
        log.info("-----------------------------------------------------");
    }

    private void executeValidate() {
        log.info("Starting Flyway validation...");
        flyway.validate(); // Throws exception on failure
        log.info("Flyway validation successful: Schema matches available migrations.");
    }

    private void executeClean() {
        log.warn("Executing Flyway clean! This will drop all objects in the configured schemas.");
        // Consider adding a confirmation prompt or --force flag in a real-world scenario
        // For now, proceed directly based on the command.
        flyway.clean();
        log.info("Flyway clean finished.");
    }


    @Override
    public int getExitCode() {
        // If this runner executed a command, return its specific exit code.
        // Otherwise, return 0 to allow normal application startup/exit.
        return commandExecuted ? this.exitCode : 0;
    }
} 