package com.vladmihalcea.hpjp.spring.transaction.jpa;

import com.vladmihalcea.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.PostComment;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.PostDetails;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.Tag;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.transaction.jpa.config.ResourceLocalReleaseAfterStatementConfiguration;
import com.vladmihalcea.hpjp.spring.transaction.jpa.service.ReleaseAfterStatementForumService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

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
@ContextConfiguration(classes = ResourceLocalReleaseAfterStatementConfiguration.class)
public class ResourceLocalReleaseAfterStatementTest extends AbstractSpringTest {

    @Autowired
    private ReleaseAfterStatementForumService releaseAfterStatementForumService;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            PostComment.class,
            PostDetails.class,
            Post.class,
            Tag.class,
        };
    }

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
