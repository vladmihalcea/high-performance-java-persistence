package com.vladmihalcea.hpjp.spring.transaction.jta.narayana;

import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.PostComment;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.PostDetails;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.Tag;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.transaction.jta.dao.TagDAO;
import com.vladmihalcea.hpjp.spring.transaction.jta.narayana.config.CamundaAsyncContinuationNarayanaJTATransactionManagerSQLServerConfiguration;
import com.vladmihalcea.hpjp.spring.transaction.jta.service.ForumService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

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
@ContextConfiguration(classes = CamundaAsyncContinuationNarayanaJTATransactionManagerSQLServerConfiguration.class)
public class CamundaAsyncContinuationNarayanaJTATransactionManagerTest extends AbstractSpringTest {

    @Autowired
    private ForumService forumService;

    @Autowired
    private TagDAO tagDAO;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ManagementService managementService;

    @Autowired
    @Qualifier("camundaDataSource")
    private DataSource camundaDataSource;

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

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Test
    public void testXATransaction() {
        // Verify that the Post was persisted via JPA within the JTA transaction
        ProcessInstance _processInstance = (ProcessInstance) transactionTemplate.execute(transactionStatus -> {
            // Start the Camunda process with variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("title", "High-Performance Java Persistence");
            variables.put("tags", new String[]{"hibernate", "jpa"});
            LOGGER.info("Execute an SQL query in PostgreSQL to see what connection we are using");
            try(Connection connection = camundaDataSource.getConnection()) {
                ResultSet resultSet = connection.createStatement().executeQuery("SELECT now() AS current_time");
                while (resultSet.next()) {
                    String timestamp = resultSet.getString(1);
                    LOGGER.info("Current time from Camunda's DataSource connection: {}", timestamp);
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
            assertNotNull(
                historyService.findHistoryCleanupJobs(),
                "The process instance should have completed"
            );

            LOGGER.info("Starting Camunda process");
            ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("forumPostProcessAsync", variables);
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

        // Execute the pending async job(s) so that callWebServiceDelegate
        // and verifyPostDelegate are invoked
        List<Job> jobs = managementService.createJobQuery()
            .processInstanceId(_processInstance.getId())
            .list();
        assertFalse(jobs.isEmpty(), "There should be at least one async job pending");
        for (Job job : jobs) {
            LOGGER.info("Executing async job: {}", job.getId());
            managementService.executeJob(job.getId());
        }

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

    @Test
    public void testAsyncStartRightAway() {
        AtomicReference<Future> asyncExecution = new AtomicReference<>();
            // Verify that the Post was persisted via JPA within the JTA transaction
        ProcessInstance _processInstance = transactionTemplate.execute(transactionStatus -> {
            // Start the Camunda process with variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("title", "High-Performance Java Persistence");
            variables.put("tags", new String[]{"hibernate", "jpa"});
            LOGGER.info("Execute an SQL query in PostgreSQL to see what connection we are using");
            try(Connection connection = camundaDataSource.getConnection()) {
                ResultSet resultSet = connection.createStatement().executeQuery("SELECT now() AS current_time");
                while (resultSet.next()) {
                    String timestamp = resultSet.getString(1);
                    LOGGER.info("Current time from Camunda's DataSource connection: {}", timestamp);
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
            assertNotNull(
                historyService.findHistoryCleanupJobs(),
                "The process instance should have completed"
            );

            LOGGER.info("Starting Camunda process");
            ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("forumPostProcessAsync", variables);

            asyncExecution.set(executorService.submit(() -> {
                transactionTemplate.execute(_transactionStatus -> {
                    // Execute the pending async job(s) so that callWebServiceDelegate
                    // and verifyPostDelegate are invoked
                    List<Job> jobs;

                    do {
                        jobs = managementService.createJobQuery()
                            .processInstanceId(processInstance.getId())
                            .list();

                        LOGGER.info("Found {} async jobs for process instance {}", jobs.size(), processInstance.getId());
                    } while (jobs.isEmpty());

                    for (Job job : jobs) {
                        try {
                            long millis = 10;
                            LOGGER.info("Sleep for {} milliseconds before executing async job: {}", millis, job.getId());
                            Thread.sleep(millis);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        LOGGER.info("Executing async job: {}", job.getId());
                        managementService.executeJob(job.getId());
                    }

                    return null;
                });
            }));

            assertNotNull(processInstance);
            return processInstance;
        });

        try {
            asyncExecution.get().get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }

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

