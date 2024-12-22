package com.vladmihalcea.hpjp.util;

import io.hypersistence.tsid.TSID;

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

        TSID_FACTORY = getTsidFactory(nodeCount);
    }

    private TsidUtils() {
        throw new UnsupportedOperationException("TsidUtils is not instantiable!");
    }

    public static TSID randomTsid() {
        return TSID_FACTORY.generate();
    }

    public static TSID.Factory getTsidFactory(int nodeCount) {
        int nodeBits = ((int) (Math.log(nodeCount) / Math.log(2))) + 1;

        return TSID.Factory.builder()
            .withRandomFunction(TSID.Factory.THREAD_LOCAL_RANDOM_FUNCTION)
            .withNodeBits(nodeBits)
            .build();
    }

    public static TSID.Factory getTsidFactory(int nodeCount, int nodeId) {
        int nodeBits = ((int) (Math.log(nodeCount) / Math.log(2))) + 1;

        return TSID.Factory.builder()
            .withRandomFunction(TSID.Factory.THREAD_LOCAL_RANDOM_FUNCTION)
            .withNodeBits(nodeBits)
            .withNode(nodeId)
            .build();
    }
}
