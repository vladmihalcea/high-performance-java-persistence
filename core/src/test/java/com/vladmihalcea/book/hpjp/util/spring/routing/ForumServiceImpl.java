package com.vladmihalcea.book.hpjp.util.spring.routing;

import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Service
public class ForumServiceImpl implements ForumService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Post newPost(String title, String... tags) {
        Post post = new Post();
        post.setTitle(title);
        entityManager.persist(post);
        return post;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> findAllByTitle(String title) {
        return entityManager.createQuery(
            "select p " +
                "from Post p " +
                "where p.title = :title", Post.class)
            .setParameter("title", title)
            .getResultList();
    }

}
