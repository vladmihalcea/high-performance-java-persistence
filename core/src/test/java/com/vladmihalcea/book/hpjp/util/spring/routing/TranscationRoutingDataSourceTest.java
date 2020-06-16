package com.vladmihalcea.book.hpjp.util.spring.routing;

import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TransactionRoutingConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TranscationRoutingDataSourceTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private ForumService forumService;



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
