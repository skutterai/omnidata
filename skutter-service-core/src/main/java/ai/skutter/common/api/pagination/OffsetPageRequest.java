package ai.skutter.common.api.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Represents an offset-based pagination request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OffsetPageRequest {

    @Min(value = 0, message = "Page number must be >= 0")
    private int page = 0;

    @Min(value = 1, message = "Page size must be >= 1")
    @Max(value = 2500, message = "Page size must be <= 2500")
    private int size = 2500;

    private String sort; // e.g., "field,asc" or "field,desc"

    public Pageable toPageable() {
        Sort sorting = Sort.unsorted();
        if (sort != null && !sort.isBlank()) {
            try {
                String[] parts = sort.split(",");
                if (parts.length == 2) {
                    Sort.Direction direction = Sort.Direction.fromString(parts[1]);
                    sorting = Sort.by(direction, parts[0]);
                } else if (parts.length == 1) {
                    sorting = Sort.by(parts[0]); // Default direction ASC
                }
            } catch (Exception e) {
                // Handle invalid sort format, potentially log or throw specific exception
                // For now, default to unsorted
            }
        }
        return PageRequest.of(page, size, sorting);
    }
} 