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
package ai.skutter.common.observability.logging;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller and actuator endpoint for managing logger levels.
 * Requires PLATFORM_OWNER role for access via /api/logging/**.
 * Access via Actuator endpoint (/actuator/skutter-loggers/**) is typically
 * secured separately in SkutterActuatorAutoConfiguration.
 */
@RestController
@RequestMapping("/api/logging")
@Endpoint(id = "skutter-loggers") // Also exposes via Actuator
@Tag(name = "Logging Management", description = "APIs for viewing and modifying logger levels.")
@SecurityRequirement(name = "bearer-jwt") // Still needed for Swagger UI auth prompt
@PreAuthorize("hasRole('PLATFORM_OWNER')") // Secure all methods in this controller
public class LoggingController {

    private static final Logger log = LoggerFactory.getLogger(LoggingController.class);
    private final LoggingSystem loggingSystem;

    public LoggingController(LoggingSystem loggingSystem) {
        this.loggingSystem = loggingSystem;
    }

    /**
     * List all available loggers and their current levels.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ReadOperation // For Actuator exposure
    @Operation(summary = "List all loggers", description = "Retrieves a list of all configured loggers and their effective/configured levels, along with the list of available log levels. Requires PLATFORM_OWNER role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of loggers",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ListLoggersResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden (Requires PLATFORM_OWNER role)", content = @Content)
    })
    public Map<String, Object> listLoggers() {
        log.debug("Listing all loggers");
        
        List<LoggerInfo> loggers = new ArrayList<>();
        
        for (LoggerConfiguration config : loggingSystem.getLoggerConfigurations()) {
            loggers.add(new LoggerInfo(
                config.getName(),
                config.getEffectiveLevel(),
                config.getConfiguredLevel()
            ));
        }
        
        return Map.of(
            "levels", Arrays.stream(LogLevel.values()).map(LogLevel::name).collect(Collectors.toList()),
            "loggers", loggers
        );
    }

    /**
     * Get a specific logger by name.
     */
    @GetMapping(value = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ReadOperation // For Actuator exposure
    @Operation(summary = "Get logger details", description = "Retrieves the details (effective and configured levels) for a specific logger by its name. Requires PLATFORM_OWNER role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved logger details",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = LoggerInfo.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden (Requires PLATFORM_OWNER role)", content = @Content),
        @ApiResponse(responseCode = "404", description = "Logger not found", 
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, 
                                   schema = @Schema(implementation = LoggerInfo.class))) 
    })
    public LoggerInfo getLogger(
            @Parameter(description = "The name of the logger (e.g., 'ROOT', 'ai.skutter')", required = true, example = "ai.skutter") 
            @PathVariable("name") @Selector String name) {
        log.debug("Getting logger: {}", name);
        
        LoggerConfiguration configuration = loggingSystem.getLoggerConfiguration(name);
        if (configuration == null) {
            log.warn("Logger not found: {}", name);
            return new LoggerInfo(name, null, null);
        }
        
        return new LoggerInfo(
            configuration.getName(),
            configuration.getEffectiveLevel(),
            configuration.getConfiguredLevel()
        );
    }

    /**
     * Update a logger's level.
     */
    @PostMapping(value = "/{name}")
    @WriteOperation // For Actuator exposure
    @Operation(summary = "Set logger level", description = "Updates the logging level for a specific logger. Set level to 'null' or omit to clear the specific level setting and inherit from the parent. Requires PLATFORM_OWNER role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated logger level",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = LoggerInfo.class))),
        @ApiResponse(responseCode = "400", description = "Invalid log level provided", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden (Requires PLATFORM_OWNER role)", content = @Content)
    })
    public LoggerInfo setLoggerLevel(
            @Parameter(description = "The name of the logger to configure (e.g., 'ROOT', 'ai.skutter')", required = true, example = "ai.skutter") 
            @PathVariable("name") @Selector String name,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON object specifying the desired log level.", required = true,
                 content = @Content(schema = @Schema(implementation = ConfigureLevelRequest.class),
                 examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\n  \"configuredLevel\": \"TRACE\"\n}"))) 
            @org.springframework.web.bind.annotation.RequestBody ConfigureLevelRequest request) {

        String level = request.getConfiguredLevel();
        log.info("Setting logger '{}' to level: {}", name, level);
        
        LogLevel logLevel = null;
        if (level != null && !level.equalsIgnoreCase("null") && !level.isBlank()) {
            try {
                 logLevel = LogLevel.valueOf(level.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Invalid log level '{}' for logger '{}'", level, name, e);
                throw new IllegalArgumentException("Invalid log level: " + level + 
                    ". Valid values are: " + Arrays.stream(LogLevel.values()).map(Enum::name).collect(Collectors.joining(", ")) + ", or null/blank to reset.");
            }
        }

        loggingSystem.setLogLevel(name, logLevel);
        
        LoggerConfiguration configuration = loggingSystem.getLoggerConfiguration(name);
        // Return the updated state
        return new LoggerInfo(
            configuration.getName(),
            configuration.getEffectiveLevel(),
            configuration.getConfiguredLevel()
        );
    }

    /**
     * Simple DTO for logger info, used in GET responses.
     */
    @Schema(description = "Details about a specific logger's configuration.")
    public static class LoggerInfo {
        @Schema(description = "The name of the logger.", example = "ai.skutter.common.security")
        private final String name;
        @Schema(description = "The effective logging level currently applied to this logger (inherited or directly set).", example = "DEBUG")
        private final String effectiveLevel;
        @Schema(description = "The level explicitly configured for this logger, or null if inheriting.", example = "DEBUG", nullable = true)
        private final String configuredLevel;

        public LoggerInfo(String name, LogLevel effectiveLevel, LogLevel configuredLevel) {
            this.name = name;
            this.effectiveLevel = effectiveLevel != null ? effectiveLevel.name() : null;
            this.configuredLevel = configuredLevel != null ? configuredLevel.name() : null;
        }

        public String getName() {
            return name;
        }

        public String getEffectiveLevel() {
            return effectiveLevel;
        }

        public String getConfiguredLevel() {
            return configuredLevel;
        }
    }
    
    /**
     * Schema definition for the response of the listLoggers endpoint.
     */
    @Schema(description = "Response containing the list of available log levels and the current state of all loggers.")
    private static class ListLoggersResponse {
        @Schema(description = "List of valid log level names that can be set.", example = "[\"OFF\", \"FATAL\", \"ERROR\", \"WARN\", \"INFO\", \"DEBUG\", \"TRACE\"]")
        private List<String> levels;
        @Schema(description = "List of all configured loggers and their current levels.")
        private List<LoggerInfo> loggers;

        // Getters required for schema generation
        public List<String> getLevels() { return levels; }
        public List<LoggerInfo> getLoggers() { return loggers; }
    }

    /**
     * DTO for the request body of the setLoggerLevel endpoint.
     * Mirrors the structure expected by the standard Actuator endpoint.
     */
    @Schema(description = "Request body for setting a logger's configured level.")
    private static class ConfigureLevelRequest {
        @Schema(description = "The log level to set (e.g., 'INFO', 'DEBUG', 'WARN', 'ERROR', 'TRACE', 'OFF', or 'null'/'empty' to reset). Case-insensitive.",
                example = "TRACE", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String configuredLevel;

        // Getter and Setter required for JSON deserialization
        public String getConfiguredLevel() { return configuredLevel; }
        public void setConfiguredLevel(String configuredLevel) { this.configuredLevel = configuredLevel; }
    }
} 