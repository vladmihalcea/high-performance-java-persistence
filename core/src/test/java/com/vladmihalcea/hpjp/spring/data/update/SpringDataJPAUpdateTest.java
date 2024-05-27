package com.vladmihalcea.hpjp.spring.data.update;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.update.config.SpringDataJPAUpdateConfiguration;
import com.vladmihalcea.hpjp.spring.data.update.domain.Post;
import com.vladmihalcea.hpjp.spring.data.update.repository.PostRepository;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionCallback;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPAUpdateConfiguration.class)
public class SpringDataJPAUpdateTest extends AbstractSpringTest {

    @Autowired
    private PostRepository postRepository;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class
        };
    }

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

