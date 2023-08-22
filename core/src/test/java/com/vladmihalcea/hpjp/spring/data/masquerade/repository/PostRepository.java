package com.vladmihalcea.hpjp.spring.data.masquerade.repository;

import com.vladmihalcea.hpjp.spring.data.masquerade.domain.Post;
import com.vladmihalcea.hpjp.spring.data.masquerade.dto.PostCommentDTO;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends BaseJpaRepository<Post, Long>, CustomPostRepository {

    @Query("""
        select new PostCommentDTO(
            pc.id,
            pc.review
        )
        from PostComment pc
        where pc.post.id = :postId
        order by pc.id
        """)
    List<PostCommentDTO> findCommentsByPost(@Param("postId") Long postId);
}
