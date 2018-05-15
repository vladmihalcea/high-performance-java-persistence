package com.sorinc.test.hibernate.one2many;


import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public abstract class AbstractProviderTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractProviderTest.class);

    static {
        System.setProperty("3a_test_active_profile", "test");
    }


    @BeforeClass
    public static void before_class() {
    }

    @AfterClass
    public static void after_class() {
    }

    @Before
    public void before_test_method() {
    }

    @After
    public void after_test_method() {
    }
}
