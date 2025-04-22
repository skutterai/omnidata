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

/**
 * Utility class for converting between different data size units.
 */
@Slf4j
public class DataSizeConverter {

    /**
     * Converts a value from one data size unit to another.
     * @param value The value to convert
     * @param fromUnit The unit to convert from
     * @param toUnit The unit to convert to
     * @return The converted value
     * @throws IllegalArgumentException if the value is negative
     */
    public static double convert(double value, DataSizeUnit fromUnit, DataSizeUnit toUnit) {
        if (value < 0) {
            String message = "Negative values are not allowed for data size conversion";
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        // Convert to bytes first (base unit)
        long bytes = (long) (value * fromUnit.getBytes());
        
        // Convert from bytes to target unit
        return (double) bytes / toUnit.getBytes();
    }
} 
