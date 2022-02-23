package com.vladmihalcea.book.hpjp.spring.transaction.routing;

import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Tag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TransactionRoutingConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TranscationRoutingDataSourceTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private ForumService forumService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Before
    public void init() {
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
