package com.vladmihalcea.hpjp.spring.data.jakarta.repository;

import com.vladmihalcea.hpjp.spring.data.jakarta.domain.Post;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import org.hibernate.StatelessSession;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends BasicRepository<Post, Long> {

    StatelessSession getSession();

    default Post persist(Post post) {
        getSession().insert(post);
        return post;
    }

    @Query("""
        select p
        from Post p
        left join fetch p.comments
        where p.title like :titlePrefix
        """)
    List<Post> findAllByTitleLike(@Param("titlePrefix") String titlePrefix);

    @Query("""
        select p
        from Post p
        left join fetch p.comments
        where p.id = :id
        """)
    Post findByIdWithComments(@Param("id") Long id);
}
