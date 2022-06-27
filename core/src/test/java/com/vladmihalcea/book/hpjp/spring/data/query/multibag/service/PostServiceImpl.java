package com.vladmihalcea.book.hpjp.spring.data.query.multibag.service;

import com.vladmihalcea.book.hpjp.spring.data.query.multibag.domain.Post;
import com.vladmihalcea.book.hpjp.spring.data.query.multibag.repository.PostRepository;
import com.vladmihalcea.book.hpjp.spring.transaction.transfer.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private PostRepository postRepository;

    @Override
    public List<Post> findAllWithCommentsAndTags(long minId, long maxId) {
        List<Post> posts = postRepository.findAllWithComments(minId, maxId);
        LOGGER.debug(
            "Fetched {} Post entities along with their associated PostComments",
            posts.size()
        );

        return postRepository.findAllWithTags(minId, maxId);
    }
}
