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

package ai.skutter.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the DeterministicIdGenerator class.
 */
class DeterministicIdGeneratorTest {

    private static final Logger log = LoggerFactory.getLogger(DeterministicIdGeneratorTest.class);

    private DeterministicIdGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new DeterministicIdGenerator();
    }

    // --- generateId Tests ---

    @Test
    void generateId_ShouldReturnSameIdForSameSimpleInput() {
        String input = "test-input";
        log.debug("Testing generateId with simple input: {}", input);
        String id1 = generator.generateId(input);
        String id2 = generator.generateId(input);
        log.debug("Generated IDs: id1={}, id2={}", id1, id2);
        assertEquals(id1, id2, "Same simple string input should generate same ID");
    }

    @Test
    void generateId_ShouldReturnDifferentIdsForDifferentSimpleInputs() {
        String input1 = "test-input-1";
        String input2 = "test-input-2";
        String id1 = generator.generateId(input1);
        String id2 = generator.generateId(input2);
        assertNotEquals(id1, id2, "Different simple string inputs should generate different IDs");
    }

    @Test
    void generateId_ShouldHandleNullInput() {
        log.debug("Testing generateId with null input");
        String id = generator.generateId(null);
        log.debug("Generated ID for null: {}", id);
        assertNotNull(id, "ID should not be null for null input");
        assertEquals(12, id.length(), "Default ID length should be 12");
        // Check that generating ID for null twice yields the same result
        assertEquals(id, generator.generateId(null), "Generating ID for null twice should yield same result");
    }

    @Test
    void generateId_ShouldHandleEmptyStringInput() {
        String id = generator.generateId("");
        assertNotNull(id, "ID should not be null for empty string input");
        assertEquals(12, id.length(), "Default ID length should be 12");
        assertEquals(id, generator.generateId(""), "Generating ID for empty string twice should yield same result");
        assertNotEquals(generator.generateId(null), id, "ID for null should differ from ID for empty string");
    }

    @Test
    void generateId_ShouldHaveDefaultLength12() {
        String input = "some-data";
        String id = generator.generateId(input);
        assertEquals(12, id.length(), "Default generated ID length should be 12");
    }

    @Test
    void generateId_ShouldProduceBase58Characters() {
        String input = "test-input-for-charset";
        String id = generator.generateId(input);
        assertTrue(id.matches("^[1-9A-HJ-NP-Za-km-z]+$"),
                "ID should only contain Base58 characters (alphanumeric excluding 0, O, I, l)");
    }

    @Test
    void generateId_ShouldHandleDifferentPrimitiveTypes() {
        String intId = generator.generateId(12345);
        String longId = generator.generateId(12345L);
        String doubleId = generator.generateId(123.45);
        String boolTrueId = generator.generateId(true);
        String boolFalseId = generator.generateId(false);

        assertEquals(intId, longId, "Integer and Long with same numeric value should generate same ID");
        assertNotEquals(intId, generator.generateId("12345"), "Number and its string representation should generate different IDs");
        assertNotEquals(boolTrueId, boolFalseId, "True and False boolean inputs should generate different IDs");
        assertNotEquals(boolTrueId, generator.generateId("true"), "Boolean and its string representation should generate different IDs");
    }

    // --- generateShortId Tests ---

    @ParameterizedTest
    @ValueSource(ints = {8, 10, 15, 20, 25, 30})
    void generateShortId_ShouldReturnIdWithSpecifiedLength(int length) {
        String input = "test-variable-length";
        log.debug("Testing generateShortId with input '{}' and length {}", input, length);
        String id = generator.generateShortId(input, length);
        log.debug("Generated short ID: {}", id);
        assertEquals(length, id.length(), "ID length should match specified length: " + length);
        assertTrue(id.matches("^[1-9A-HJ-NP-Za-km-z]+$"), "Short ID should only contain Base58 characters");
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 31, 0, -5})
    void generateShortId_ShouldThrowExceptionForInvalidLength(int invalidLength) {
        String input = "test-invalid-length";
        assertThrows(IllegalArgumentException.class,
                () -> generator.generateShortId(input, invalidLength),
                "Should throw IllegalArgumentException for length outside [8, 30]: " + invalidLength);
    }

    @Test
    void generateShortId_ShouldBeDeterministicForSameLength() {
        String input = "consistent-short-id";
        String id1 = generator.generateShortId(input, 10);
        String id2 = generator.generateShortId(input, 10);
        assertEquals(id1, id2, "generateShortId should be deterministic for the same input and length");
    }

    @Test
    void generateShortId_ShouldDifferForDifferentLengths() {
        String input = "different-lengths";
        String id10 = generator.generateShortId(input, 10);
        String id15 = generator.generateShortId(input, 15);
        assertNotEquals(id10, id15, "IDs generated with different lengths should differ");
        // Note: Not guaranteed to be non-prefixes due to Base58 padding/truncation
    }

    // --- generateUniqueId Tests ---

    @Test
    void generateUniqueId_ShouldCombineBaseIdAndSuffix() {
        String input = "base-for-unique";
        String suffix = "suffix123";
        log.debug("Testing generateUniqueId with input '{}' and suffix '{}'", input, suffix);
        String uniqueId = generator.generateUniqueId(input, suffix);
        log.debug("Generated unique ID: {}", uniqueId);

        assertTrue(uniqueId.contains("-"), "Unique ID should contain a hyphen separator");
        assertTrue(uniqueId.endsWith("-" + suffix), "Unique ID should end with the hyphen and suffix");

        // Check base ID part length (default 9 for unique IDs)
        String basePart = uniqueId.substring(0, uniqueId.indexOf("-"));
        assertEquals(9, basePart.length(), "Base part of unique ID should have length 9");
        assertEquals(basePart, generator.generateShortId(input, 9), "Base part should match generateShortId(input, 9)");
    }

    @Test
    void generateUniqueId_ShouldThrowExceptionForNullOrEmptySuffix() {
        String input = "test-suffix-validation";
        assertThrows(IllegalArgumentException.class,
                () -> generator.generateUniqueId(input, null),
                "Should throw IllegalArgumentException for null suffix");
        assertThrows(IllegalArgumentException.class,
                () -> generator.generateUniqueId(input, ""),
                "Should throw IllegalArgumentException for empty suffix");
    }

    @Test
    void generateUniqueId_ShouldBeDeterministicForBasePart() {
        String input = "deterministic-unique-base";
        String suffix1 = "sfx1";
        String suffix2 = "sfx2";

        String uniqueId1 = generator.generateUniqueId(input, suffix1);
        String uniqueId2 = generator.generateUniqueId(input, suffix2);

        String basePart1 = uniqueId1.substring(0, uniqueId1.indexOf("-"));
        String basePart2 = uniqueId2.substring(0, uniqueId2.indexOf("-"));

        assertEquals(basePart1, basePart2, "Base part should be deterministic for the same input");
    }

    // --- Canonicalization Tests ---

    @Test
    void generateId_ShouldHandleComplexObjectsConsistently() {
        Map<String, Object> input1 = new LinkedHashMap<>(); // Use LinkedHashMap for predictable iteration order initially
        input1.put("name", "test");
        input1.put("count", 123);
        input1.put("enabled", true);
        input1.put("tags", Arrays.asList("a", "b"));

        Map<String, Object> input2 = new HashMap<>(); // Use HashMap to test key ordering
        input2.put("enabled", true);
        input2.put("count", 123);
        input2.put("name", "test");
        input2.put("tags", Arrays.asList("a", "b"));

        String id1 = generator.generateId(input1);
        String id2 = generator.generateId(input2);

        assertEquals(id1, id2, "Same complex object content (regardless of map impl/order) should generate same ID due to key sorting");
    }

    @Test
    void generateId_ShouldHandleNestedStructuresConsistently() {
        Map<String, Object> nested1 = Map.of("innerKey", "innerValue", "innerCount", 456);
        Map<String, Object> input1 = Map.of("name", "test", "nested", nested1);

        // Same structure, different map instance
        Map<String, Object> nested2 = Map.of("innerCount", 456, "innerKey", "innerValue");
        Map<String, Object> input2 = Map.of("nested", nested2, "name", "test");

        String id1 = generator.generateId(input1);
        String id2 = generator.generateId(input2);

        assertEquals(id1, id2, "Same nested structure should generate same ID regardless of map order");
    }

    @Test
    void generateId_ShouldHandleCollectionsConsistently_ListOrderMatters() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        List<String> list3 = Arrays.asList("c", "b", "a"); // Different order

        String id1 = generator.generateId(list1);
        String id2 = generator.generateId(list2);
        String id3 = generator.generateId(list3);

        assertEquals(id1, id2, "Same ordered lists should generate same ID");
        assertNotEquals(id1, id3, "Differently ordered lists should generate different IDs");
    }

    @Test
    void generateId_ShouldHandleCollectionsConsistently_SetOrderDoesNotMatter() {
        // Use elements that are not naturally in alphabetical order to test sorting
        Set<String> set1 = new HashSet<>(Arrays.asList("c", "a", "b"));
        List<String> list = Arrays.asList("c", "a", "b"); // Use the same initial unsorted order as set1
        log.debug("Testing generateId with Set {} and List {}", set1, list);

        String id1 = generator.generateId(set1);
        String listId = generator.generateId(list);
        
        log.debug("Set ID (expected based on sorted elements): {}", id1);
        log.debug("List ID (expected based on original order): {}", listId);

        assertEquals(id1, generator.generateId(new HashSet<>(Arrays.asList("b", "c", "a")))); // Check determinism

        assertNotEquals(id1, listId, "Set (sorted canonical) and List (ordered canonical) with same elements should generate different IDs");
    }

    @Test
    void generateId_ShouldHandleJsonStringsAsEquivalentToParsedObjects() {
        // JSON string with specific order
        String jsonStr1 = "{\"name\":\"test\", \"count\":123, \"enabled\":true}";
        // JSON string with different field order
        String jsonStr2 = "{\"count\":123, \"enabled\":true, \"name\":\"test\"}";
        // Equivalent Map object
        Map<String, Object> mapObj = Map.of("name", "test", "count", 123, "enabled", true);

        String idJson1 = generator.generateId(jsonStr1);
        String idJson2 = generator.generateId(jsonStr2);
        String idMap = generator.generateId(mapObj);

        assertEquals(idJson1, idJson2, "JSON strings with same content should generate same ID regardless of internal field order");
        assertEquals(idJson1, idMap, "JSON string and its equivalent Map object should generate the same ID");
    }

    @Test
    void generateId_ShouldHandleNonJsonStringsDifferently() {
        String simpleString = "This is a plain string";
        String jsonLikeString = "{not really json}";
        String emptyObjectJson = "{}";

        String idSimple = generator.generateId(simpleString);
        String idJsonLike = generator.generateId(jsonLikeString);
        String idEmptyObj = generator.generateId(emptyObjectJson);
        String idActualMap = generator.generateId(new HashMap<>());

        assertNotEquals(idSimple, idJsonLike, "Plain string and JSON-like string should differ");
        assertNotEquals(idJsonLike, idEmptyObj, "Malformed JSON-like string and empty JSON object string should differ");
        assertEquals(idEmptyObj, idActualMap, "Empty JSON object string and empty Map should produce same ID");
    }

    // --- Edge Case Tests ---

    @Test
    void generateId_ShouldHandleSpecialCharactersInStrings() {
        String input1 = "string with !@#$%^&*()_+=-`~[]{}\\|;:'\",./<>?";
        String id1 = generator.generateId(input1);
        assertNotNull(id1);
        assertEquals(id1, generator.generateId(input1)); // Deterministic
    }

    @Test
    void generateId_ShouldHandleUnicodeCharactersInStrings() {
        String input1 = "こんにちは世界"; // Japanese
        String input2 = "你好，世界"; // Chinese
        String input3 = "😊 Emoji Test";

        String id1 = generator.generateId(input1);
        String id2 = generator.generateId(input2);
        String id3 = generator.generateId(input3);

        assertNotNull(id1);
        assertNotNull(id2);
        assertNotNull(id3);
        assertNotEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertEquals(id1, generator.generateId(input1)); // Deterministic
    }
} 