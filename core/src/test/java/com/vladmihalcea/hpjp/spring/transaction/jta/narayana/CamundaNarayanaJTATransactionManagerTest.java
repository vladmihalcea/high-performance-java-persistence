package com.vladmihalcea.hpjp.spring.transaction.jta.narayana;

import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.PostComment;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.PostDetails;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.Tag;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.transaction.jta.dao.TagDAO;
import com.vladmihalcea.hpjp.spring.transaction.jta.narayana.config.CamundaNarayanaJTATransactionManagerSQLServerConfiguration;
import com.vladmihalcea.hpjp.spring.transaction.jta.service.ForumService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test demonstrating Camunda BPM with Narayana JTA transactions
 * and SQL Server.
 * <p>
 * The test starts a Camunda process that creates a forum Post via JPA
 * within a JTA transaction managed by Narayana, then verifies that both
 * the Camunda process instance and the JPA entity were committed atomically.
 *
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = CamundaNarayanaJTATransactionManagerSQLServerConfiguration.class)
public class CamundaNarayanaJTATransactionManagerTest extends AbstractSpringTest {

    @Autowired
    private ForumService forumService;

    @Autowired
    private TagDAO tagDAO;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            PostComment.class,
            PostDetails.class,
            Post.class,
            Tag.class,
        };
    }

    @Override
    public void afterInit() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                Tag hibernate = new Tag();
                hibernate.setName("hibernate");
                tagDAO.persist(hibernate);

                Tag jpa = new Tag();
                jpa.setName("jpa");
                tagDAO.persist(jpa);

                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void test() {
        // Start the Camunda process with variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", "High-Performance Java Persistence");
        variables.put("tags", new String[]{"hibernate", "jpa"});

        ProcessInstance processInstance = runtimeService
            .startProcessInstanceByKey("forumPostProcess", variables);

        assertNotNull(processInstance);
        LOGGER.info(
            "Started Camunda process instance with id: {}",
            processInstance.getId()
        );

        // The process should have completed (no user tasks)
        HistoricProcessInstance historicProcessInstance = historyService
            .createHistoricProcessInstanceQuery()
            .processInstanceId(processInstance.getId())
            .singleResult();

        assertNotNull(historicProcessInstance);
        assertNotNull(
            historicProcessInstance.getEndTime(),
            "The process instance should have completed"
        );

        // Verify that the Post was persisted via JPA within the JTA transaction
        transactionTemplate.execute(transactionStatus -> {
            List<Post> posts = forumService.findAllByTitle(
                "High-Performance Java Persistence"
            );
            assertEquals(1, posts.size());

            Post post = posts.get(0);
            assertNotNull(post.getId());
            assertEquals("High-Performance Java Persistence", post.getTitle());
            assertFalse(post.getTags().isEmpty());
            assertEquals(2, post.getTags().size());

            LOGGER.info(
                "Post [id={}, title='{}'] was created with {} tags via Camunda process using JTA",
                post.getId(),
                post.getTitle(),
                post.getTags().size()
            );

            return null;
        });
    }

    @Test
    public void testXATransaction() {
        // Verify that the Post was persisted via JPA within the JTA transaction
        ProcessInstance _processInstance = transactionTemplate.execute(transactionStatus -> {
            // Start the Camunda process with variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("title", "High-Performance Java Persistence");
            variables.put("tags", new String[]{"hibernate", "jpa"});

            ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("forumPostProcess", variables);

            assertNotNull(processInstance);
            LOGGER.info(
                "Started Camunda process instance with id: {}",
                processInstance.getId()
            );

            List<Post> posts = forumService.findAllByTitle(
                "High-Performance Java Persistence"
            );
            assertEquals(1, posts.size());

            Post post = posts.get(0);
            assertNotNull(post.getId());
            assertEquals("High-Performance Java Persistence", post.getTitle());
            assertFalse(post.getTags().isEmpty());
            assertEquals(2, post.getTags().size());

            LOGGER.info(
                "Post [id={}, title='{}'] was created with {} tags via Camunda process using JTA",
                post.getId(),
                post.getTitle(),
                post.getTags().size()
            );

            return processInstance;
        });

        // The process should have completed (no user tasks)
        HistoricProcessInstance historicProcessInstance = historyService
            .createHistoricProcessInstanceQuery()
            .processInstanceId(_processInstance.getId())
            .singleResult();

        assertNotNull(historicProcessInstance);
        assertNotNull(
            historicProcessInstance.getEndTime(),
            "The process instance should have completed"
        );
    }
}

