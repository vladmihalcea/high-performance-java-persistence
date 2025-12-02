package com.vladmihalcea.hpjp.spring.data.bytecode;

import com.vladmihalcea.hpjp.hibernate.forum.Attachment;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.bytecode.config.SpringDataJPARuntimeBytecodeEnhancementConfiguration;
import com.vladmihalcea.hpjp.spring.data.bytecode.repository.AttachmentRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.net.URISyntaxException;

/**
 * @author Vlad Mihalcea
 */
@Disabled
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
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>${maven-surefire-plugin.version}</version>
        <configuration>
            <argLine>-javaagent:d:/.m2/repository/org/springframework/spring-instrument/6.2.14/spring-instrument-6.2.14.jar</argLine>
            <systemPropertyVariables>
                <hibernate.testing.bytecode.enhancement.extension.engine.enabled>true</hibernate.testing.bytecode.enhancement.extension.engine.enabled>
            </systemPropertyVariables>
        </configuration>
    </plugin>
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

