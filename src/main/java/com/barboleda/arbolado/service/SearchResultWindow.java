package com.barboleda.arbolado.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.barboleda.arbolado.domain.ArbolResponse;
import com.barboleda.arbolado.domain.SearchLimits;

/**
 * Slices fetched results at MAX_ITEMS (1000). Truncation indicates >1000 trees
 * within the 1080m padded query (e.g. large parks); client must handle truncated.
 */
@Component
public class SearchResultWindow
{

    /**
     * Slices the fetched list to the maximum allowed items.
     *
     * @param fetched the raw fetched DTOs, possibly exceeding the limit
     * @return a result pair containing the sliced items and whether truncation occurred
     */
    public WindowResult window(List<ArbolResponse> fetched)
    {
        boolean truncated = fetched.size() > SearchLimits.MAX_ITEMS;
        List<ArbolResponse> items = truncated ? List.copyOf(fetched.subList(0, SearchLimits.MAX_ITEMS))
                : List.copyOf(fetched);
        return new WindowResult(items, truncated);
    }

    /**
     * Pair of sliced results plus truncation flag.
     *
     * @param items the sliced items
     * @param truncated true if the original result exceeded the limit
     */
    public record WindowResult(List<ArbolResponse> items, boolean truncated)
    {
    }
}
