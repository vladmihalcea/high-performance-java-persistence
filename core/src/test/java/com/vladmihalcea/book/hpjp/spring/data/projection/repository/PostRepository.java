package com.vladmihalcea.book.hpjp.spring.data.projection.repository;

import com.vladmihalcea.book.hpjp.spring.data.projection.domain.Post;
import com.vladmihalcea.book.hpjp.spring.data.projection.dto.PostCommentDTO;
import com.vladmihalcea.book.hpjp.spring.data.projection.dto.PostCommentRecord;
import com.vladmihalcea.book.hpjp.spring.data.projection.dto.PostCommentSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.Tuple;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long>, CustomPostRepository {

    @Query("""
        select 
            p.id as id, 
            p.title as title, 
            c.review as review
        from PostComment c
        join c.post p
        where p.title like :postTitle
        order by c.id
        """)
    List<Tuple> findCommentTupleByTitle(@Param("postTitle") String postTitle);

    @Query("""
        select 
            p.id as id, 
            p.title as title, 
            c.review as review
        from PostComment c
        join c.post p
        where p.title like :postTitle
        order by c.id
        """)
    List<PostCommentSummary> findCommentSummaryByTitle(@Param("postTitle") String postTitle);

    @Query("""
        select new PostCommentDTO(
            p.id as id, 
            p.title as title, 
            c.review as review
        )
        from PostComment c
        join c.post p
        where p.title like :postTitle
        order by c.id
        """)
    List<PostCommentDTO> findCommentDTOByTitle(@Param("postTitle") String postTitle);

    @Query("""
        select new PostCommentRecord(
            p.id as id, 
            p.title as title, 
            c.review as review
        )
        from PostComment c
        join c.post p
        where p.title like :postTitle
        order by c.id
        """)
    List<PostCommentRecord> findCommentRecordByTitle(@Param("postTitle") String postTitle);
}
