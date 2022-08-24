package com.vladmihalcea.book.hpjp.spring.data.projection;

import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.transformer.PostDTO;
import com.vladmihalcea.book.hpjp.spring.data.projection.config.SpringDataJPAProjectionConfiguration;
import com.vladmihalcea.book.hpjp.spring.data.projection.domain.Post;
import com.vladmihalcea.book.hpjp.spring.data.projection.domain.PostComment;
import com.vladmihalcea.book.hpjp.spring.data.projection.dto.PostCommentDTO;
import com.vladmihalcea.book.hpjp.spring.data.projection.dto.PostCommentRecord;
import com.vladmihalcea.book.hpjp.spring.data.projection.dto.PostCommentSummary;
import com.vladmihalcea.book.hpjp.spring.data.projection.repository.PostRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPAProjectionConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPAProjectionTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PostRepository postRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public static final int POST_COUNT = 50;
    public static final int POST_COMMENT_COUNT = 5;

    @Before
    public void init() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                final AtomicLong commentId = new AtomicLong(0);

                LongStream.rangeClosed(1, POST_COUNT).forEach(i -> {
                    Post post = new Post()
                        .setId(i)
                        .setTitle(String.format("High-Performance Java Persistence, Chapter nr. %d", i));

                    LongStream.rangeClosed(1, POST_COMMENT_COUNT).forEach(j -> {
                        post.addComment(
                            new PostComment()
                                .setId(commentId.incrementAndGet())
                                .setReview(
                                    String.format("Good review nr. %d", commentId.get())
                                )
                        );

                    });
                    entityManager.persist(post);
                });

                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void test() {
        String titleToken = "High-Performance Java Persistence%";

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            List<Tuple> commentTuples = postRepository.findCommentTupleByTitle(titleToken);

            assertFalse(commentTuples.isEmpty());

            Tuple commentTuple = commentTuples.get(0);
            assertEquals(1L, ((Number) commentTuple.get("id")).longValue());
            assertTrue(((String) commentTuple.get("title")).contains("Chapter nr. 1"));

            List<PostCommentSummary> commentSummaries = postRepository.findCommentSummaryByTitle(titleToken);

            assertFalse(commentSummaries.isEmpty());

            PostCommentSummary commentSummary = commentSummaries.get(0);
            assertEquals(Long.valueOf(1), commentSummary.getId());
            assertTrue(commentSummary.getTitle().contains("Chapter nr. 1"));

            List<PostCommentDTO> commentDTOs = postRepository.findCommentDTOByTitle(titleToken);

            assertFalse(commentDTOs.isEmpty());

            PostCommentDTO commentDTO = commentDTOs.get(0);
            assertEquals(Long.valueOf(1), commentDTO.getId());
            assertTrue(commentDTO.getTitle().contains("Chapter nr. 1"));
            assertEquals(
                commentDTO,
                new PostCommentDTO(
                    commentDTO.getId(),
                    commentDTO.getTitle(),
                    commentDTO.getReview()
                )
            );

            List<PostCommentRecord> commentRecords = postRepository.findCommentRecordByTitle(titleToken);

            assertFalse(commentRecords.isEmpty());

            PostCommentRecord commentRecord = commentRecords.get(0);
            assertEquals(Long.valueOf(1), commentRecord.id());
            assertTrue(commentRecord.title().contains("Chapter nr. 1"));
            assertEquals(
                commentRecord,
                new PostCommentRecord(
                    commentRecord.id(),
                    commentRecord.title(),
                    commentRecord.review()
                )
            );

            List<PostDTO> postDTOs = postRepository.findPostDTOByTitle(titleToken);

            assertEquals(POST_COUNT, postDTOs.size());

            PostDTO postDTO = postDTOs.get(0);
            assertEquals(Long.valueOf(1), postDTO.getId());
            assertTrue(postDTO.getTitle().contains("Chapter nr. 1"));
            assertEquals(POST_COMMENT_COUNT, postDTO.getComments().size());

            return null;
        });
    }
}

