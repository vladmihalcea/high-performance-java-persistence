package com.vladmihalcea.book.hpjp.spring.transaction.routing;

import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Tag;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class ForumServiceImpl implements ForumService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Post newPost(String title, String... tags) {
        Post post = new Post();
        post.setTitle(title);

        post.getTags().addAll(
            entityManager.createQuery("""
                select t
                from Tag t
                where t.name in :tags
                """, Tag.class)
            .setParameter("tags", Arrays.asList(tags))
            .getResultList()
        );

        entityManager.persist(post);

        return post;
    }

    @Override
    public List<Post> findAllPostsByTitle(String title) {
        return entityManager.createQuery("""
            select p
            from Post p
            where p.title = :title
            """, Post.class)
        .setParameter("title", title)
        .getResultList();
    }
}
