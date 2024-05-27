package com.vladmihalcea.hpjp.spring.data.masquerade;

import com.blazebit.persistence.PagedList;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.masquerade.config.SpringDataJPAMasqueradeConfiguration;
import com.vladmihalcea.hpjp.spring.data.masquerade.domain.Post;
import com.vladmihalcea.hpjp.spring.data.masquerade.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.masquerade.dto.PostCommentDTO;
import com.vladmihalcea.hpjp.spring.data.masquerade.dto.PostDTO;
import com.vladmihalcea.hpjp.spring.data.masquerade.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.data.masquerade.service.ForumService;
import com.vladmihalcea.hpjp.util.CryptoUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPAMasqueradeConfiguration.class)
public class SpringDataJPAMasqueradeTest extends AbstractSpringTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public static final int POST_COUNT = 50;

    public static final int PAGE_SIZE = 25;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ForumService forumService;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            PostComment.class,
            Post.class,
        };
    }

    @Override
    public void afterInit() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                LocalDateTime timestamp = LocalDateTime.of(
                    2021, 12, 30, 12, 0, 0, 0
                );

                LongStream.rangeClosed(1, POST_COUNT).forEach(postId -> {
                    Post post = new Post()
                        .setId(postId)
                        .setTitle(
                            String.format("High-Performance Java Persistence - Chapter %d",
                                postId)
                        )
                        .setCreatedOn(
                            timestamp.plusMinutes(postId)
                        );

                    postRepository.persist(post);
                });

                Post post = postRepository.getReferenceById(1L);
                LongStream.rangeClosed(1, 10).forEach(postCommentId -> {
                    entityManager.persist(
                        new PostComment()
                            .setId(postCommentId)
                            .setPost(post)
                            .setReview(
                                String.format("Comment nr %d", postCommentId)
                            )
                    );
                });
                
                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void test() {
        PagedList<PostDTO> topPage = forumService.firstLatestPosts(PAGE_SIZE);

        assertEquals(POST_COUNT, topPage.getTotalSize());
        assertEquals(POST_COUNT / PAGE_SIZE, topPage.getTotalPages());
        assertEquals(1, topPage.getPage());
        List<String> topIds = topPage.stream()
            .map(PostDTO::getId)
            .toList();
        assertEquals(
            "3qEiB21WnB/yQ4muQe6cpw==",
            topIds.get(0)
        );
        assertEquals(
            Long.valueOf(50),
            CryptoUtils.decrypt(topIds.get(0), Long.class)
        );

        assertEquals(
            "9jfsI1A92KIzd34ZfRxgtQ==",
            topIds.get(1)
        );
        assertEquals(
            Long.valueOf(49),
            CryptoUtils.decrypt(topIds.get(1), Long.class)
        );

        LOGGER.info("Top ids: {}", topIds);

        PagedList<PostDTO> nextPage = forumService.findNextLatestPosts(topPage);

        assertEquals(2, nextPage.getPage());

        List<String> nextIds = nextPage.stream()
            .map(PostDTO::getId)
            .toList();
        assertEquals(Long.valueOf(25), CryptoUtils.decrypt(nextIds.get(0), Long.class));
        assertEquals(Long.valueOf(24), CryptoUtils.decrypt(nextIds.get(1), Long.class));

        LOGGER.info("Next ids: {}", nextIds);

        PostDTO firstPost = nextPage.get(nextPage.getSize() - 1);
        List<PostCommentDTO> comments = forumService.findCommentsByPost(
            firstPost.getId()
        );

        assertEquals(
            10,
            comments.size()
        );
        assertEquals(
            "ltAKs4jLw8N7q7SHeUR2Kw==",
            comments.get(0).getId()
        );
        assertEquals(
            Long.valueOf(1),
            CryptoUtils.decrypt(comments.get(0).getId(), Long.class)
        );
    }
}

