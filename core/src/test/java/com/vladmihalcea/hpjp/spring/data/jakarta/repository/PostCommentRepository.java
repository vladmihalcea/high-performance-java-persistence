package com.vladmihalcea.hpjp.spring.data.jakarta.repository;

import com.vladmihalcea.hpjp.spring.data.jakarta.domain.Post;
import com.vladmihalcea.hpjp.spring.data.jakarta.domain.PostComment;
import jakarta.data.repository.*;
import org.hibernate.StatelessSession;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostCommentRepository extends BasicRepository<PostComment, Long> {

    StatelessSession getSession();

    default PostComment persist(PostComment comment) {
        getSession().insert(comment);
        return comment;
    }

    @Find
    List<PostComment> findByReview(String review);

    @Query("""
        select pc
        from PostComment pc
        join fetch pc.post p
        join fetch p.comments
        where p.title like :titlePrefix
        """)
    List<PostComment> findAllWithPostTitleLike(@Param("titlePrefix") String titlePrefix);

    @Query("""
        select pc
        from PostComment pc
        join fetch pc.post p
        where pc.id between :minId and :maxId
        """)
    List<PostComment> findAllWithPostByIds(
        @Param("minId") Long minId,
        @Param("maxId") Long maxId
    );

    @Query("""
        delete from PostComment c
        where c.post in :posts
        """)
    void deleteAllByPosts(@Param("posts") List<Post> posts);
}
