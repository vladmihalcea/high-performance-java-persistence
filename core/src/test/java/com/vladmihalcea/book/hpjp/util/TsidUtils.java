package com.vladmihalcea.book.hpjp.util;

import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * <code>TsidUtils</code> - Tsid utilities holder.
 *
 * @author Vlad Mihalcea
 */
public class TsidUtils {
    public static final String TSID_NODE_COUNT_PROPERTY = "tsid.node.count";
    public static final String TSID_NODE_COUNT_ENV = "TSID_NODE_COUNT";

    public static TsidFactory TSID_FACTORY;

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

        TSID_FACTORY = TsidFactory.builder()
            .withRandomFunction(length -> {
                final byte[] bytes = new byte[length];
                ThreadLocalRandom.current().nextBytes(bytes);
                return bytes;
            })
            .withNodeBits(nodeBits)
            .build();
    }

    private TsidUtils() {
        throw new UnsupportedOperationException("TsidUtils is not instantiable!");
    }

    public static Tsid randomTsid() {
        return TSID_FACTORY.create();
    }
}
