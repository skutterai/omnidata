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
package ai.skutter.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.commons.net.util.SubnetUtils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

@Slf4j
public class TypeIdentifier {
    private static final Pattern MAC_ADDRESS_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DERIVED_IDENTIFIER_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INVALID_FILENAME_CHARS = Pattern.compile("[<>\"|?*\\x00-\\x1F]");
    private static final Pattern FILENAME_WITH_EXTENSION = Pattern.compile("^[^./\\\\](?:[^/\\\\]*)[^/\\\\.]$|^[^./\\\\](?:[^/\\\\]*)(?:\\.\\w+)?$");
    private static final Pattern UNIX_PATH = Pattern.compile("^(/[^/\\x00:*?\"<>|\\r\\n]*)+$");
    private static final Pattern RELATIVE_PATH = Pattern.compile("^(?:[^/\\\\:*?\"<>|\\x00-\\x1F]+(?:/[^/\\\\:*?\"<>|\\x00-\\x1F]+)*)?$");
    private static final int MAX_PATH_COMPONENT_LENGTH = 255;
    private static final EmailValidator emailValidator = EmailValidator.getInstance();
    private static final InetAddressValidator inetAddressValidator = InetAddressValidator.getInstance();
    private static final DomainValidator domainValidator = DomainValidator.getInstance();
    private static final UrlValidator urlValidator = new UrlValidator(
        new String[]{"http", "https", "ftp"},  // Allowed schemes
        UrlValidator.ALLOW_2_SLASHES +          // Allow double slashes in path
        UrlValidator.ALLOW_ALL_SCHEMES +        // Allow all schemes
        UrlValidator.NO_FRAGMENTS +             // Don't allow fragments
        UrlValidator.ALLOW_LOCAL_URLS           // Allow local URLs
    );

    // RFC1918 private network ranges
    private static final String[] RFC1918_RANGES = {
        "10.0.0.0/8",      // Class A private network
        "172.16.0.0/12",   // Class B private network
        "192.168.0.0/16"   // Class C private network
    };

    private static final Pattern WINDOWS_RESERVED_NAMES = Pattern.compile("^(con|prn|aux|nul|com[1-9]|lpt[1-9])(\\..*)?$", Pattern.CASE_INSENSITIVE);

    private static final Pattern INVALID_PATH_CHARS = Pattern.compile("[<>\"|?*:\\x00-\\x1F]");
    private static final Pattern INVALID_COLON_PATTERN = Pattern.compile("(?<!^[A-Za-z]):");

    public static boolean isEmail(String input) {
        try {
            if (StringUtils.isBlank(input)) {
                log.warn("Input string is null or empty");
                return false;
            }
            return emailValidator.isValid(input);
        } catch (Exception e) {
            log.error("Error validating email: {}", e.getMessage());
            return false;
        }
    }

    public static boolean isFQDN(String input) {
        try {
            if (StringUtils.isBlank(input)) {
                log.warn("Input string is null or empty");
                return false;
            }
            return domainValidator.isValid(input);
        } catch (Exception e) {
            log.error("Error validating FQDN: {}", e.getMessage());
            return false;
        }
    }

    public static boolean isIPv4(String input) {
        try {
            if (StringUtils.isBlank(input)) {
                log.warn("Input string is null or empty");
                return false;
            }
            return inetAddressValidator.isValidInet4Address(input);
        } catch (Exception e) {
            log.error("Error validating IPv4: {}", e.getMessage());
            return false;
        }
    }

    public static boolean isIPv6(String input) {
        try {
            if (StringUtils.isBlank(input)) {
                log.warn("Input string is null or empty");
                return false;
            }
            return inetAddressValidator.isValidInet6Address(input);
        } catch (Exception e) {
            log.error("Error validating IPv6: {}", e.getMessage());
            return false;
        }
    }

    public static boolean isMacAddress(String input) {
        try {
            if (StringUtils.isBlank(input)) {
                log.warn("Input string is null or empty");
                return false;
            }
            return MAC_ADDRESS_PATTERN.matcher(input).matches();
        } catch (Exception e) {
            log.error("Error validating MAC address: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if an IPv4 address is a private address according to RFC1918.
     * @param input The IPv4 address to check
     * @return true if the address is private, false otherwise
     */
    public static boolean isPrivateIPv4(String input) {
        try {
            if (!isIPv4(input)) {
                return false;
            }

            for (String range : RFC1918_RANGES) {
                try {
                    SubnetUtils subnet = new SubnetUtils(range);
                    subnet.setInclusiveHostCount(true);  // Include network and broadcast addresses
                    if (subnet.getInfo().isInRange(input)) {
                        return true;
                    }
                } catch (IllegalArgumentException e) {
                    log.error("Error checking subnet range {}: {}", range, e.getMessage());
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error checking private IPv4: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if an IPv6 address is a private address according to RFC4193 (Unique Local Address)
     * or RFC4291 (Link-Local Address).
     * @param input The IPv6 address to check
     * @return true if the address is private, false otherwise
     */
    public static boolean isPrivateIPv6(String input) {
        try {
            if (!isIPv6(input)) {
                return false;
            }

            InetAddress addr = InetAddress.getByName(input);
            if (!(addr instanceof Inet6Address)) {
                return false;
            }

            byte[] bytes = addr.getAddress();
            
            // Check if it's a Unique Local Address (ULA) - starts with fc00::/7
            if ((bytes[0] & 0xfe) == 0xfc) {
                return true;
            }
            
            // Check if it's a Link-Local Address - starts with fe80::/10
            if ((bytes[0] & 0xff) == 0xfe && (bytes[1] & 0xc0) == 0x80) {
                return true;
            }
            
            return false;
        } catch (UnknownHostException e) {
            log.error("Error checking private IPv6: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates if a string is a valid URL.
     * This method supports common URL schemes (http, https, ftp) and validates
     * the URL structure, including the protocol, domain, and path components.
     *
     * @param input The URL string to validate
     * @return true if the input is a valid URL, false otherwise
     */
    public static boolean isUrl(String input) {
        try {
            if (StringUtils.isBlank(input)) {
                log.warn("Input string is null or empty");
                return false;
            }
            return urlValidator.isValid(input);
        } catch (Exception e) {
            log.error("Error validating URL: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates if a string is a valid UUID.
     * This method checks if the input string matches the standard UUID format
     * (8-4-4-4-12 hexadecimal digits, with hyphens).
     *
     * @param input The string to validate
     * @return true if the input is a valid UUID, false otherwise
     */
    public static boolean isUUID(String input) {
        try {
            if (StringUtils.isBlank(input)) {
                log.warn("Input string is null or empty");
                return false;
            }
            // First check the format with regex
            if (!UUID_PATTERN.matcher(input).matches()) {
                log.debug("Input does not match UUID format");
                return false;
            }
            // Then try to parse it as a UUID
            java.util.UUID.fromString(input);
            return true;
        } catch (IllegalArgumentException e) {
            log.debug("Invalid UUID format: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error validating UUID: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a string is a valid derived identifier.
     * A derived identifier is a deterministic UUID (version 4) that follows a specific format:
     * - 8 hexadecimal digits
     * - hyphen
     * - 4 hexadecimal digits
     * - hyphen
     * - 4 hexadecimal digits (starting with 4)
     * - hyphen
     * - 4 hexadecimal digits (starting with 8, 9, a, or b)
     * - hyphen
     * - 12 hexadecimal digits
     *
     * @param input The string to validate
     * @return true if the input is a valid derived identifier, false otherwise
     */
    public static boolean isDerivedIdentifier(String input) {
        try {
            if (StringUtils.isBlank(input)) {
                log.warn("Input string is null or empty");
                return false;
            }
            // Check if it matches the derived identifier pattern
            if (!DERIVED_IDENTIFIER_PATTERN.matcher(input).matches()) {
                log.debug("Input does not match derived identifier format");
                return false;
            }
            // Additional validation: ensure it's a valid UUID
            java.util.UUID.fromString(input);
            return true;
        } catch (IllegalArgumentException e) {
            log.debug("Invalid derived identifier format: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error validating derived identifier: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the given string is a valid filename with directory path.
     * Uses java.nio.file.Path for basic validation and adds simple checks for:
     * - Invalid characters in filename
     * - Maximum path component length
     * - Leading/trailing spaces and dots
     *
     * @param pathWithFilename The path including filename to validate
     * @return true if both the path and filename are valid, false otherwise
     */
    public static boolean isValidFilePathWithName(String pathWithFilename) {
        try {
            if (StringUtils.isBlank(pathWithFilename)) {
                log.warn("Path with filename is null or empty");
                return false;
            }

            Path path = Paths.get(pathWithFilename);
            
            // Check for invalid colons (except for Windows drive letters)
            if (INVALID_COLON_PATTERN.matcher(pathWithFilename).find()) {
                log.warn("Path contains invalid colon: {}", pathWithFilename);
                return false;
            }

            // Check each path component
            for (Path component : path) {
                String name = component.toString();
                
                // Skip empty components (e.g., from multiple slashes)
                if (name.isEmpty()) {
                    continue;
                }

                // Check for invalid characters
                if (INVALID_FILENAME_CHARS.matcher(name).find()) {
                    log.warn("Path component contains invalid characters: {}", name);
                    return false;
                }

                // Check component length
                if (name.length() > MAX_PATH_COMPONENT_LENGTH) {
                    log.warn("Path component exceeds maximum length ({}): {}", MAX_PATH_COMPONENT_LENGTH, name);
                    return false;
                }

                // Check for spaces at start/end
                if (name.startsWith(" ") || name.endsWith(" ")) {
                    log.warn("Path component cannot start or end with spaces: {}", name);
                    return false;
                }

                // Check for leading or trailing dots
                if (name.startsWith(".") || name.endsWith(".")) {
                    log.warn("Path component cannot start or end with dots: {}", name);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating file path with name: {}", pathWithFilename, e);
            return false;
        }
    }

    /**
     * Validates if a string is a valid directory path.
     * Uses java.nio.file.Path for basic validation and adds simple checks for:
     * - Invalid characters in path components
     * - Maximum path component length
     * - Leading/trailing spaces and dots
     *
     * @param path The directory path to validate
     * @return true if the path is valid, false otherwise
     */
    public static boolean isValidDirectoryPath(String path) {
        try {
            if (StringUtils.isBlank(path)) {
                log.warn("Directory path is null or empty");
                return false;
            }

            Path dirPath = Paths.get(path);
            
            // Check each path component
            for (Path component : dirPath) {
                String name = component.toString();
                
                // Skip empty components (e.g., from multiple slashes)
                if (name.isEmpty()) {
                    continue;
                }

                // Check for invalid characters
                if (INVALID_FILENAME_CHARS.matcher(name).find()) {
                    log.warn("Path component contains invalid characters: {}", name);
                    return false;
                }

                // Check component length
                if (name.length() > MAX_PATH_COMPONENT_LENGTH) {
                    log.warn("Path component exceeds maximum length ({}): {}", MAX_PATH_COMPONENT_LENGTH, name);
                    return false;
                }

                // Check for spaces at start/end
                if (name.startsWith(" ") || name.endsWith(" ")) {
                    log.warn("Path component cannot start or end with spaces: {}", name);
                    return false;
                }

                // Check for trailing dots
                if (name.endsWith(".")) {
                    log.warn("Path component cannot end with dots: {}", name);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating directory path: {}", path, e);
            return false;
        }
    }
} 