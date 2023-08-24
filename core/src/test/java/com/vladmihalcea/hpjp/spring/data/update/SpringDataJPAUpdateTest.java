package com.vladmihalcea.hpjp.spring.data.update;

import com.vladmihalcea.hpjp.spring.data.update.domain.Post;
import com.vladmihalcea.hpjp.spring.data.update.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.data.update.config.SpringDataJPAUpdateConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPAUpdateConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPAUpdateTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    public void testDefaultUpdate() {
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            postRepository.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
            );

            postRepository.persist(
                new Post()
                    .setId(2L)
                    .setTitle("Java Persistence with Hibernate")
            );
            
            return null;
        });
        
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Post post1 = postRepository.findById(1L).orElseThrow();
            post1.setTitle("High-Performance Java Persistence 2nd Edition");

            Post post2 = postRepository.findById(2L).orElseThrow();
            post2.setLikes(12);

            postRepository.flush();
            return null;
        });
    }
}

