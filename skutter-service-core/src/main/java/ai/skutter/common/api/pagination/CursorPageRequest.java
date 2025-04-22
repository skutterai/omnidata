package ai.skutter.common.api.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

/**
 * Represents a cursor-based pagination request.
 * The `cursor` value is opaque and typically represents the value of the
 * sort field for the last item in the previous page.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageRequest {

    private String cursor;

    @Min(value = 1, message = "Limit must be >= 1")
    @Max(value = 2500, message = "Limit must be <= 2500")
    private int limit = 2500;

    private SortDirection direction = SortDirection.AFTER;

    private Sort sort;

    public enum SortDirection {
        BEFORE, AFTER, ASCENDING
    }
    
    /**
     * Checks if this is a request for the page *after* the cursor.
     * @return true if requesting the next page, false otherwise.
     */
    public boolean isAfter() {
        return direction == SortDirection.AFTER;
    }

    /**
     * Checks if this is a request for the page *before* the cursor.
     * @return true if requesting the previous page, false otherwise.
     */
    public boolean isBefore() {
        return direction == SortDirection.BEFORE;
    }

    public static CursorPageRequest of(String cursor, int size) {
        return of(cursor, size, Sort.unsorted());
    }

    public static CursorPageRequest of(String cursor, int size, Sort sort) {
        return new CursorPageRequest(cursor, size, SortDirection.ASCENDING, sort);
    }

    public String getCursor() {
        return cursor;
    }

    public int getSize() {
        return limit;
    }

    public Sort getSort() {
        return sort;
    }
} 