package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.jpa.dao.tmp;

import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.transaction.spring.jpa.dao.GenericDAOImpl;
import org.springframework.stereotype.Repository;

/**
 * <code>PostDAOImpl</code> - Post DAO Impl
 *
 * @author Vlad Mihalcea
 */
@Repository
public class PostDAOImpl extends GenericDAOImpl<Post, Long> implements PostDAO {

    protected PostDAOImpl() {
        super(Post.class);
    }
}
