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

import java.util.Date;

/**
 * Utility class for converting between different time units.
 */
@Slf4j
public class TimeUnitConverter {
    /**
     * Converts a value from one time unit to another.
     * @param value The value to convert
     * @param fromUnit The unit to convert from
     * @param toUnit The unit to convert to
     * @return The converted value
     * @throws IllegalArgumentException if the value is negative
     */
    public static double convert(double value, TimeUnit fromUnit, TimeUnit toUnit) {
        if (value < 0) {
            String message = "Negative values are not allowed for time conversion";
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        // Convert to microseconds first (base unit)
        long microseconds = (long) (value * fromUnit.getMicroseconds());
        
        // Convert from microseconds to target unit
        return (double) microseconds / toUnit.getMicroseconds();
    }

    /**
     * Converts a Date object to a value in the specified time unit.
     * @param date The Date object to convert
     * @param toUnit The unit to convert to
     * @return The converted value
     * @throws IllegalArgumentException if the date is null
     */
    public static double convert(Date date, TimeUnit toUnit) {
        if (date == null) {
            String message = "Date cannot be null";
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        // Convert to microseconds first (base unit)
        long microseconds = date.getTime() * 1000L; // Convert milliseconds to microseconds
        
        // Convert from microseconds to target unit
        return (double) microseconds / toUnit.getMicroseconds();
    }

    /**
     * Converts a Date object to epoch time (seconds since January 1, 1970, 00:00:00 GMT).
     * @param date The Date object to convert
     * @return The epoch time in seconds
     * @throws IllegalArgumentException if the date is null
     */
    public static long toEpochSeconds(Date date) {
        if (date == null) {
            String message = "Date cannot be null";
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        return date.getTime() / 1000L; // Convert milliseconds to seconds
    }

    /**
     * Converts epoch time (seconds since January 1, 1970, 00:00:00 GMT) to a Date object.
     * @param epochSeconds The epoch time in seconds
     * @return The corresponding Date object
     * @throws IllegalArgumentException if the epoch time is negative
     */
    public static Date fromEpochSeconds(long epochSeconds) {
        if (epochSeconds < 0) {
            String message = "Epoch time cannot be negative";
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        return new Date(epochSeconds * 1000L); // Convert seconds to milliseconds
    }
} 
