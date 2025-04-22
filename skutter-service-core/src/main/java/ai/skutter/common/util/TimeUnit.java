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

/**
 * Enum representing different units of time.
 */
public enum TimeUnit {
    MICROSECONDS(1L),
    MILLISECONDS(1000L),
    SECONDS(1000L * 1000L),
    MINUTES(60L * 1000L * 1000L),
    HOURS(60L * 60L * 1000L * 1000L),
    DAYS(24L * 60L * 60L * 1000L * 1000L),
    WEEKS(7L * 24L * 60L * 60L * 1000L * 1000L),
    YEARS(365L * 24L * 60L * 60L * 1000L * 1000L);

    private final long microseconds;

    TimeUnit(long microseconds) {
        this.microseconds = microseconds;
    }

    public long getMicroseconds() {
        return microseconds;
    }
} 
