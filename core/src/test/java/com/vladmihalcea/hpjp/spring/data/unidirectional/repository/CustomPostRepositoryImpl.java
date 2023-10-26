package com.vladmihalcea.hpjp.spring.data.unidirectional.repository;

import com.vladmihalcea.hpjp.spring.data.unidirectional.domain.Post;
import com.vladmihalcea.hpjp.spring.data.unidirectional.domain.PostDetails;
import jakarta.persistence.EntityManager;

/**
 * @author Vlad Mihalcea
 */
public class CustomPostRepositoryImpl implements CustomPostRepository<Long> {

    private final PostDetailsRepository postDetailsRepository;

    private final UserVoteRepository userVoteRepository;

    private final PostCommentRepository postCommentRepository;

    private final PostTagRepository postTagRepository;

    private final EntityManager entityManager;

    public CustomPostRepositoryImpl(
            PostDetailsRepository postDetailsRepository,
            UserVoteRepository userVoteRepository, 
            PostCommentRepository postCommentRepository,
            PostTagRepository postTagRepository,
            EntityManager entityManager) {
        this.postDetailsRepository = postDetailsRepository;
        this.userVoteRepository = userVoteRepository;
        this.postCommentRepository = postCommentRepository;
        this.postTagRepository = postTagRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void deleteById(Long postId) {
        postDetailsRepository.deleteByPostId(postId);
        userVoteRepository.deleteAllByPostId(postId);
        postCommentRepository.deleteAllByPostId(postId);
        postTagRepository.deleteAllByPostId(postId);

        entityManager.createQuery("""
            delete from Post
            where id = :postId
            """)
        .setParameter("postId", postId)
        .executeUpdate();
    }
}
