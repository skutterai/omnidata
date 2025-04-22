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
import ai.skutter.common.util.exception.NotEnoughMemoryException;

/**
 * Utility class for monitoring JVM memory usage and checking against specified limits.
 */
@Slf4j
public class CheckMemoryLimit {
    private static final Runtime runtime = Runtime.getRuntime();

    /**
     * Checks if current memory usage exceeds the specified percentage limit.
     * If the limit is exceeded, throws a NotEnoughMemoryException.
     *
     * @param maxMemoryUsedPercent Maximum allowed memory usage as a percentage (0-100)
     * @throws NotEnoughMemoryException if memory usage exceeds the specified limit
     */
    public static void checkMemoryUsage(double maxMemoryUsedPercent) {
        if (maxMemoryUsedPercent <= 0) {
            return; // No limit specified, skip check
        }

        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double usedPercent = (double) usedMemory / totalMemory * 100;

        log.debug("Memory usage: {}/{} bytes ({}%)", usedMemory, totalMemory, String.format("%.2f", usedPercent));

        if (usedPercent > maxMemoryUsedPercent) {
            String message = String.format(
                "Not enough memory to meet current demands. Memory used is %.2f%% (limit is %.2f%%). Total memory is %,.2f MB, used memory is %,.2f MB",
                usedPercent,
                maxMemoryUsedPercent,
                DataSizeConverter.convert(totalMemory, DataSizeUnit.BYTES, DataSizeUnit.MEGABYTES),
                DataSizeConverter.convert(usedMemory, DataSizeUnit.BYTES, DataSizeUnit.MEGABYTES)
            );
            throw new NotEnoughMemoryException(message);
        }
    }
} 