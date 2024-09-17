package com.vladmihalcea.hpjp.spring.data.audit.service;

import com.vladmihalcea.hpjp.spring.data.audit.domain.Post;
import com.vladmihalcea.hpjp.spring.data.audit.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.audit.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.audit.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Transactional
    public Post savePost(Post post) {
        return postRepository.save(post);
    }

    @Transactional
    public Post savePostAndComments(Post post, PostComment... comments) {
        post = postRepository.save(post);

        if(comments.length > 0) {
            postCommentRepository.saveAll(Arrays.asList(comments));
        }

        return post;
    }

    @Transactional
    public void deletePost(Post post) {
        postCommentRepository.deleteByPost(post);
        postRepository.delete(post);
    }
}
