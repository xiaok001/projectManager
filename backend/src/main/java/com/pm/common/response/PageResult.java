package com.pm.common.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Generic paginated result wrapper.
 *
 * @param <T> the element type in the result list
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The data records for the current page. */
    private List<T> list;

    /** Total number of records matching the query. */
    private long total;

    /** Current page number (1-based). */
    private int pageNum;

    /** Number of records per page. */
    private int pageSize;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, int pageNum, int pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    /**
     * Calculate the total number of pages.
     *
     * @return total pages
     */
    public int getTotalPages() {
        if (pageSize <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / pageSize);
    }
}
