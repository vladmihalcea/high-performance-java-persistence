package com.vladmihalcea.book.hpjp.util;

import io.hypersistence.tsid.TSID;

import java.util.concurrent.ThreadLocalRandom;

/**
 * <code>TsidUtils</code> - Tsid utilities holder.
 *
 * @author Vlad Mihalcea
 */
public class TsidUtils {
    public static final String TSID_NODE_COUNT_PROPERTY = "tsid.node.count";
    public static final String TSID_NODE_COUNT_ENV = "TSID_NODE_COUNT";

    public static TSID.Factory TSID_FACTORY;

    static {
        String nodeCountSetting = System.getProperty(
            TSID_NODE_COUNT_PROPERTY
        );
        if (nodeCountSetting == null) {
            nodeCountSetting = System.getenv(
                TSID_NODE_COUNT_ENV
            );
        }

        int nodeCount = nodeCountSetting != null ?
            Integer.parseInt(nodeCountSetting) :
            256;

        int nodeBits = (int) (Math.log(nodeCount) / Math.log(2));

        TSID_FACTORY = TSID.Factory.builder()
            .withRandomFunction(TSID.Factory.THREAD_LOCAL_RANDOM_FUNCTION)
            .withNodeBits(nodeBits)
            .build();
    }

    private TsidUtils() {
        throw new UnsupportedOperationException("TsidUtils is not instantiable!");
    }

    public static TSID randomTsid() {
        return TSID_FACTORY.generate();
    }
}
