package com.vladmihalcea.book.hpjp.spring.transaction.jpa;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.book.hpjp.spring.transaction.jpa.config.ResourceLocalReleaseAfterStatementConfiguration;
import com.vladmihalcea.book.hpjp.spring.transaction.jpa.service.ReleaseAfterStatementForumService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

/**
 * This test case demonstrates what happens when you enable the AFTER_STATEMENT
 * Hibernate connection release mode when using a RESOURCE_LOCAL JPA transaction
 * with Spring.
 *
 * Currently, this mode is disabled, as it will make the test fail.
 * To enable the AFTER_STATEMENT release mode, open the {@link ResourceLocalReleaseAfterStatementConfiguration}
 * file and pass the {@code DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT} setting to the
 * {@code CONNECTION_HANDLING} Hibernate configuration property.
 *
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ResourceLocalReleaseAfterStatementConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ResourceLocalReleaseAfterStatementTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private ReleaseAfterStatementForumService releaseAfterStatementForumService;

    @Test
    public void test() {
        Post newPost = releaseAfterStatementForumService.newPost(
            "High-Performance Java Persistence"
        );

        /*
         * At this point, if we enable the {@code DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT}
         * connection release mode, there won't be any Post available because the previous @Transactional
         * block did not commit the database transaction for the same JDBC Connection that was used
         * to persist the Post entity.
         *
         * So, basically, the Post entity is persisted using one JDBC Connection, which is also sent
         * back to the pool after the flush is done, and by the time the TransactionInterceptor
         * tries to commit the connection, no {@code physicalConnection} will be found in
         * {@link LogicalConnectionManagedImpl}, so a new JDBC Connection will be fetched from the pool
         * only to commit that instead of the one that contained the modifications.
         */

        PostDTO postDTO = releaseAfterStatementForumService.savePostTitle(
            newPost.getId(),
            "High-Performance Java Persistence, 2nd edition"
        );

        assertEquals(
            "High-Performance Java Persistence, 2nd edition",
            postDTO.getTitle()
        );
    }
}
