package com.vladmihalcea.hpjp.spring.data.bytecode;

import com.vladmihalcea.hpjp.hibernate.forum.Attachment;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.bytecode.config.SpringDataJPARuntimeBytecodeEnhancementConfiguration;
import com.vladmihalcea.hpjp.spring.data.bytecode.repository.AttachmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.net.URISyntaxException;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPARuntimeBytecodeEnhancementConfiguration.class)
public class SpringDataJPARuntimeBytecodeEnhancementTest extends AbstractSpringTest {

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Attachment.class
        };
    }

    /*
     * Run like this:
     *
     * java -Djavaagent=d:/.m2/repository/org/springframework/spring-instrument/6.2.14/spring-instrument-6.2.14.jar
     */
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

