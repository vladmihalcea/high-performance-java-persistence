package com.vladmihalcea.hpjp.spring.data.recursive.repository;

import com.vladmihalcea.hpjp.spring.data.recursive.domain.Post;
import com.vladmihalcea.hpjp.spring.data.recursive.domain.PostCommentDTO;
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

    @Query(value = """
        select new PostCommentDTO(
            id,
            parent.id,
            review,
            createdOn,
            score
        )
        from PostComment pc
        where post.id = :postId
        order by id
        """
    )
    List<PostCommentDTO> findAllCommentDTOsByPost(@Param("postId") Long postId);
}
