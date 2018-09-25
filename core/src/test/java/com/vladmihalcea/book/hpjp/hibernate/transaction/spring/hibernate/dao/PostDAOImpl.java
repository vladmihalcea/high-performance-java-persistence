package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.hibernate.dao;

import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public class PostDAOImpl extends GenericDAOImpl<Post, Long> implements PostDAO {

    protected PostDAOImpl() {
        super(Post.class);
    }

    @Override
    public List<Post> findByTitle(String title) {
        return getSession().createQuery(
            "select p from Post p where p.title = :title", Post.class)
        .setParameter("title", title)
        .getResultList();
    }
}
