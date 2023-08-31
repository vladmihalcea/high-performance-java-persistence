package com.vladmihalcea.hpjp.spring.data.projection.repository;

import com.vladmihalcea.hpjp.spring.data.projection.domain.Post;
import com.vladmihalcea.hpjp.spring.data.projection.dto.PostCommentDTO;
import com.vladmihalcea.hpjp.spring.data.projection.dto.PostCommentRecord;
import com.vladmihalcea.hpjp.spring.data.projection.dto.PostCommentSummary;
import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    List<Tuple> findAllCommentTuplesByPostTitle(@Param("postTitle") String postTitle);

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
    List<PostCommentSummary> findAllCommentSummariesByPostTitle(@Param("postTitle") String postTitle);

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
    List<PostCommentDTO> findCommentDTOByPostTitle(@Param("postTitle") String postTitle);

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
    List<PostCommentRecord> findCommentRecordByPostTitle(@Param("postTitle") String postTitle);
}
