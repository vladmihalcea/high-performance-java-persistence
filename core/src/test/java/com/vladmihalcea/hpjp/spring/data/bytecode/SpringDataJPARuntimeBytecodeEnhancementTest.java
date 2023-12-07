package com.vladmihalcea.hpjp.spring.data.bytecode;

import com.vladmihalcea.hpjp.spring.data.bytecode.config.SpringDataJPARuntimeBytecodeEnhancementConfiguration;
import com.vladmihalcea.hpjp.spring.data.bytecode.repository.AttachmentRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URISyntaxException;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPARuntimeBytecodeEnhancementConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPARuntimeBytecodeEnhancementTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Test
    public void test() throws URISyntaxException {
        AttachmentLazyLoading logic = new AttachmentLazyLoading(
            transactionTemplate,
            attachmentRepository
        );
        //Needed in order to delay the Attachment class loging
        logic.test();
    }
}

