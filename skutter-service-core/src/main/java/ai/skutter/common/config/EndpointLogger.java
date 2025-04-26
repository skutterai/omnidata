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
package ai.skutter.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Logs registered Spring MVC endpoint mappings on application startup.
 */
@Component
public class EndpointLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(EndpointLogger.class);

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("+----------------------------------------------------------------------+");
        log.info("| Registered Application Endpoints                                     |");
        log.info("+----------------------------------------------------------------------+");

        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();

        handlerMethods.forEach((info, handlerMethod) -> {
            // Get URL patterns - compatible with Spring Boot 3.x
            Set<String> patterns = getPatterns(info);
            
            // Extract HTTP methods
            Set<String> methodSet = !info.getMethodsCondition().isEmpty() ? 
                    info.getMethodsCondition().getMethods().stream().map(Enum::name).collect(Collectors.toSet()) : 
                    Collections.singleton("ANY");
            
            String methods = String.join(", ", methodSet);
            
            // Get Controller and Method name
            String controllerName = handlerMethod.getBeanType().getSimpleName();
            String methodName = handlerMethod.getMethod().getName();

            // Log each pattern
            if (patterns.isEmpty()) {
                // If we couldn't extract patterns using standard methods, try to use toString()
                // which often contains the pattern information
                String infoString = info.toString();
                if (infoString.contains("{") && infoString.contains("}")) {
                    log.info("| Mapped \"{}\" [{}] onto {}.{}", 
                            infoString, 
                            methods, 
                            controllerName, 
                            methodName);
                } else {
                    log.info("| Mapping with pattern info: {} [{}] onto {}.{}", 
                            infoString, 
                            methods, 
                            controllerName, 
                            methodName);
                }
            } else {
                patterns.forEach(pattern -> 
                    log.info("| Mapped \"{}\" [{}] onto {}.{}", 
                            pattern, 
                            methods, 
                            controllerName, 
                            methodName)
                );
            }
        });
        log.info("+----------------------------------------------------------------------+");
    }

    /**
     * Extract patterns from RequestMappingInfo in a way that works with both Spring Boot 2.x and 3.x
     */
    private Set<String> getPatterns(RequestMappingInfo info) {
        Set<String> patterns = new LinkedHashSet<>();
        
        try {
            // First try Spring Boot 3.x style - getPathPatternsCondition()
            // This uses reflection because the method might not be available in all versions
            try {
                var method = info.getClass().getMethod("getPathPatternsCondition");
                var pathPatternsCondition = method.invoke(info);
                if (pathPatternsCondition != null) {
                    var getPatternsMethod = pathPatternsCondition.getClass().getMethod("getPatterns");
                    var pathPatterns = (Set<?>) getPatternsMethod.invoke(pathPatternsCondition);
                    
                    if (pathPatterns != null && !pathPatterns.isEmpty()) {
                        patterns.addAll(pathPatterns.stream()
                                .map(Object::toString)
                                .collect(Collectors.toSet()));
                        return patterns;
                    }
                }
            } catch (Exception e) {
                // Fallback to Spring Boot 2.x style if the 3.x method isn't available
                // or fails for any reason
            }
            
            // Spring Boot 2.x style - getPatternsCondition()
            if (info.getPatternsCondition() != null) {
                patterns.addAll(info.getPatternsCondition().getPatterns());
                return patterns;
            }
            
            // Last resort: Try to extract patterns from the string representation
            String infoString = info.toString();
            if (infoString.contains("{") && infoString.contains("}")) {
                int start = infoString.indexOf('{');
                int end = infoString.indexOf('}', start);
                if (start >= 0 && end > start) {
                    String patternString = infoString.substring(start + 1, end).trim();
                    if (!patternString.isEmpty()) {
                        patterns.add(patternString);
                    }
                }
            }
            
            return patterns;
        } catch (Exception e) {
            log.debug("Failed to extract patterns from RequestMappingInfo: {}", e.getMessage());
            return Collections.emptySet();
        }
    }
}