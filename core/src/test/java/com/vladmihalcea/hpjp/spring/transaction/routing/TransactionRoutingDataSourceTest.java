package com.vladmihalcea.hpjp.spring.transaction.routing;

import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.PostComment;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.PostDetails;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.Tag;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

@ContextConfiguration(classes = TransactionRoutingConfiguration.class)
public class TransactionRoutingDataSourceTest extends AbstractSpringTest {

    @Autowired
    private ForumService forumService;

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
        transactionTemplate.execute(status -> {
            Tag jdbc = new Tag();
            jdbc.setName("JDBC");
            entityManager.persist(jdbc);

            Tag jpa = new Tag();
            jpa.setName("JPA");
            entityManager.persist(jpa);

            Tag hibernate = new Tag();
            hibernate.setName("Hibernate");
            entityManager.persist(hibernate);

            return null;
        });
    }

    @Test
    public void test() {
        Post post = forumService.newPost(
            "High-Performance Java Persistence",
            "JDBC", "JPA", "Hibernate"
        );

        List<Post> posts = forumService.findAllPostsByTitle(
            "High-Performance Java Persistence"
        );
    }
}
