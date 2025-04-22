package ai.skutter.common.api.pagination;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Standardized response for paginated data, including HATEOAS links.
 *
 * @param <T> The type of the content in the page.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PagedResponse<T> extends RepresentationModel<PagedResponse<T>> {

    private List<T> content;
    private PageMetadata metadata;

    public PagedResponse(Page<T> page, UriComponentsBuilder uriBuilder) {
        this.content = page.getContent();
        this.metadata = new PageMetadata(page);
        addPaginationLinks(page, uriBuilder);
    }

    public PagedResponse(List<T> content, Cursor cursor, UriComponentsBuilder uriBuilder) {
        this.content = content;
        this.metadata = new PageMetadata(cursor, content.size());
        addCursorLinks(cursor, uriBuilder);
    }

    private void addPaginationLinks(Page<T> page, UriComponentsBuilder uriBuilder) {
        // Self link
        add(Link.of(uriBuilder.replaceQueryParam("page", page.getNumber()).replaceQueryParam("size", page.getSize()).toUriString(), IanaLinkRelations.SELF.value()));

        // First link
        add(Link.of(uriBuilder.replaceQueryParam("page", 0).toUriString(), IanaLinkRelations.FIRST.value()));

        // Last link
        if (page.getTotalPages() > 0) {
            add(Link.of(uriBuilder.replaceQueryParam("page", page.getTotalPages() - 1).toUriString(), IanaLinkRelations.LAST.value()));
        }

        // Next link
        if (page.hasNext()) {
            add(Link.of(uriBuilder.replaceQueryParam("page", page.getNumber() + 1).toUriString(), IanaLinkRelations.NEXT.value()));
        }

        // Previous link
        if (page.hasPrevious()) {
            add(Link.of(uriBuilder.replaceQueryParam("page", page.getNumber() - 1).toUriString(), IanaLinkRelations.PREV.value()));
        }
    }

    private void addCursorLinks(Cursor cursor, UriComponentsBuilder uriBuilder) {
        // Self link - reflects the current request that generated this response
        UriComponentsBuilder selfBuilder = uriBuilder;
        if (cursor.getCurrent() != null) {
            selfBuilder = selfBuilder.replaceQueryParam("cursor", cursor.getCurrent());
        }
        add(Link.of(selfBuilder.toUriString(), IanaLinkRelations.SELF.value()));

        // Next link
        if (cursor.getNext() != null) {
            add(Link.of(uriBuilder.replaceQueryParam("cursor", cursor.getNext()).replaceQueryParam("direction", "AFTER").toUriString(), IanaLinkRelations.NEXT.value()));
        }

        // Previous link
        if (cursor.getPrevious() != null) {
            add(Link.of(uriBuilder.replaceQueryParam("cursor", cursor.getPrevious()).replaceQueryParam("direction", "BEFORE").toUriString(), IanaLinkRelations.PREV.value()));
        }
    }

    @Data
    public static class PageMetadata {
        private int size;
        @JsonProperty("total_elements")
        private Long totalElements;
        @JsonProperty("total_pages")
        private Integer totalPages;
        private int number; // Current page number (for offset)
        @JsonProperty("has_next")
        private Boolean hasNext;
        @JsonProperty("has_previous")
        private Boolean hasPrevious;
        private String nextCursor;
        private String previousCursor;

        // Constructor for Offset Pagination
        public PageMetadata(Page<?> page) {
            this.size = page.getSize();
            this.totalElements = page.getTotalElements();
            this.totalPages = page.getTotalPages();
            this.number = page.getNumber();
            this.hasNext = page.hasNext();
            this.hasPrevious = page.hasPrevious();
        }

        // Constructor for Cursor Pagination
        public PageMetadata(Cursor cursor, int currentSize) {
            this.size = currentSize;
            this.hasNext = cursor.isHasNext();
            this.hasPrevious = cursor.isHasPrevious();
            this.nextCursor = cursor.getNext();
            this.previousCursor = cursor.getPrevious();
            // Total elements/pages are generally not available/meaningful in pure cursor pagination
            this.totalElements = null;
            this.totalPages = null;
            this.number = 0; // Not applicable for cursor
        }
    }

    @Data
    public static class Cursor {
        private String current;
        private String next;
        private String previous;
        private boolean hasNext;
        private boolean hasPrevious;

        public Cursor(String current, String next, String previous, boolean hasNext, boolean hasPrevious) {
            this.current = current;
            this.next = next;
            this.previous = previous;
            this.hasNext = hasNext;
            this.hasPrevious = hasPrevious;
        }
    }
} 