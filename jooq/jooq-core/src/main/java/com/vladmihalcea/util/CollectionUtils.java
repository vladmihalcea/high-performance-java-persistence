package com.vladmihalcea.util;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * <code>CollectionUtils</code> - Collection utilities holder.
 *
 * @author Vlad Mihalcea
 */
public final class CollectionUtils {

    /**
     * Prevent any instantiation.
     */
    private CollectionUtils() {
        throw new UnsupportedOperationException("The " + getClass() + " is not instantiable!");
    }

    /**
     * Split an element collection into batches.
     *
     * @param elements elements to split in batches
     * @param <T>       class type
     * @return the Stream of batches
     */
    public static <T> Stream<List<T>> spitInBatches(List<T> elements, int batchSize) {
        int elementCount = elements.size();
        if (elementCount <= 0) {
            return Stream.empty();
        }
        int batchCount = (elementCount - 1) / batchSize;
        return IntStream.range(0, batchCount + 1)
            .mapToObj(
                batchNumber -> elements.subList(
                    batchNumber * batchSize,
                    batchNumber == batchCount ? elementCount : (batchNumber + 1) * batchSize
                )
            );
    }
}
