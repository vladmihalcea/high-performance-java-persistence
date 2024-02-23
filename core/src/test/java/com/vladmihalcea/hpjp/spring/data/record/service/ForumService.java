package com.vladmihalcea.hpjp.spring.data.record.service;

import com.vladmihalcea.hpjp.spring.data.record.domain.Post;
import com.vladmihalcea.hpjp.spring.data.record.domain.PostRecord;
import com.vladmihalcea.hpjp.spring.data.record.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class ForumService {

    @Autowired
    private PostRepository postRepository;

    public PostRecord findPostRecordById(Long postId) {
        return postRepository
            .findWithCommentsById(postId)
            .map(Post::toRecord)
            .orElse(null);
    }

    @Transactional
    public PostRecord insertPostRecord(PostRecord postRecord) {
        return postRepository.persist(postRecord.toPost()).toRecord();
    }

    @Transactional
    public PostRecord mergePostRecord(PostRecord postRecord) {
        return postRepository.merge(postRecord.toPost()).toRecord();
    }
}
