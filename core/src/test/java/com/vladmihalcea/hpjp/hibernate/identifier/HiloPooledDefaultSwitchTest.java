package com.vladmihalcea.hpjp.hibernate.identifier;

import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class HiloPooledDefaultSwitchTest {

    private HiloIdentifierTest hiloIdentifierTest = new HiloIdentifierTest();

    private PooledSequenceIdentifierTest pooledIdentifierTest = new PooledSequenceIdentifierTest() {
        @Override
        protected void additionalProperties(Properties properties) {
            properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "none");
        }
    };

    @Test
    public void testDefaultSwitch() {
        try {
            hiloIdentifierTest.init();
            hiloIdentifierTest.testHiloIdentifierGenerator();

            try {
                pooledIdentifierTest.init();

                fail("Should have thrown MappingException");
            } catch (Exception e) {
                assertEquals(
                    "The increment size of the [post_sequence] sequence is set to [3] in the entity mapping while the associated database sequence increment size is [1].",
                    ExceptionUtil.rootCause(e).getMessage()
                );
            }
        } finally {
            hiloIdentifierTest.destroy();
        }
    }

}
