package com.vladmihalcea.hpjp.spring.transaction.jpa.repository;

import com.vladmihalcea.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;
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
        select p
        from Post p
        where p.title = :title
        """
    )
    List<Post> findByTitle(@Param("title") String title);

    @Query("""
        select new PostDTO(p.id, p.title)
        from Post p
        where p.id = :id
        """
    )
    PostDTO getPostDTOById(@Param("id") Long id);
}
