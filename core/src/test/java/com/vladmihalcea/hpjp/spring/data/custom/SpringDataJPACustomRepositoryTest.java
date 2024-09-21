package com.vladmihalcea.hpjp.spring.data.custom;

import com.vladmihalcea.hpjp.hibernate.forum.Post;
import com.vladmihalcea.hpjp.hibernate.forum.PostComment;
import com.vladmihalcea.hpjp.hibernate.forum.Tag;
import com.vladmihalcea.hpjp.hibernate.query.dto.projection.transformer.PostDTO;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.custom.config.SpringDataJPACustomRepositoryConfiguration;
import com.vladmihalcea.hpjp.spring.data.custom.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.data.custom.service.ForumService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPACustomRepositoryConfiguration.class)
public class SpringDataJPACustomRepositoryTest extends AbstractSpringTest {

    @Autowired
    private ForumService forumService;

    @Autowired
    private PostRepository postRepository;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            PostComment.class,
            Post.class,
            Tag.class
        };
    }

    @Test
    public void testResultTransfromer() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                entityManager.persist(
                    new Post()
                        .setId(1L)
                        .setTitle("High-Performance Java Persistence")
                        .addComment(
                            new PostComment()
                                .setId(1L)
                                .setReview("Best book on JPA and Hibernate!")
                        )
                        .addComment(
                            new PostComment()
                                .setId(2L)
                                .setReview("A must-read for every Java developer!")
                        )
                );

                entityManager.persist(
                    new Post()
                        .setId(2L)
                        .setTitle("Hypersistence Optimizer")
                        .addComment(
                            new PostComment()
                                .setId(3L)
                                .setReview("It's like pair programming with Vlad!")
                        )
                );
                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }

        List<PostDTO> postDTOs = forumService.findPostDTOWithComments();

        assertEquals(2, postDTOs.size());
        assertEquals(2, postDTOs.get(0).getComments().size());
        assertEquals(1, postDTOs.get(1).getComments().size());

        PostDTO post1DTO = postDTOs.get(0);

        assertEquals(1L, post1DTO.getId().longValue());
        assertEquals(2, post1DTO.getComments().size());
        assertEquals(1L, post1DTO.getComments().get(0).getId().longValue());
        assertEquals(2L, post1DTO.getComments().get(1).getId().longValue());

        PostDTO post2DTO = postDTOs.get(1);

        assertEquals(2L, post2DTO.getId().longValue());
        assertEquals(1, post2DTO.getComments().size());
        assertEquals(3L, post2DTO.getComments().get(0).getId().longValue());
    }

    @Test
    public void testSaveAntiPattern() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                entityManager.persist(
                    new Post()
                        .setId(1L)
                        .setTitle("High-Performance Java Persistence")
                        .addComment(
                            new PostComment()
                                .setId(1L)
                                .setReview("Best book on JPA and Hibernate!")
                        )
                        .addComment(
                            new PostComment()
                                .setId(2L)
                                .setReview("A must-read for every Java developer!")
                        )
                );

                entityManager.persist(
                    new Post()
                        .setId(2L)
                        .setTitle("Hypersistence Optimizer")
                        .addComment(
                            new PostComment()
                                .setId(3L)
                                .setReview("It's like pair programming with Vlad!")
                        )
                );
                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }

        forumService.saveAntiPattern(1L, "Hack!");
    }

    @Test
    public void testFindAllAntiPattern() {
        int POST_SIZE = 50;

        List<Tag> tags = List.of(
            new Tag()
                .setId(1L)
                .setName("JDBC"),
            new Tag()
                .setId(2L)
                .setName("JPA"),
            new Tag()
                .setId(3L)
                .setName("Hibernate")
        );

        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {

                tags.forEach(tag -> entityManager.persist(tag));

                for (long i = 1; i <= POST_SIZE; i++) {
                    entityManager.persist(
                        new Post()
                            .setId(i)
                            .setTitle(
                                String.format(
                                    "High-Performance Java Persistence, Part %d",
                                    i
                                )
                            )
                            .addTag(tags.get((int) i % 3))
                    );
                }

                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            List<String> matchingTags = List.of("JPA", "Hibernate");
            LOGGER.info("Fetch post titles using a single query");
            List<String> postTitleQueryRecords = postRepository.findPostTitleByTags(matchingTags);
            assertEquals(
                BigDecimal.valueOf(POST_SIZE)
                    .multiply(BigDecimal.valueOf(matchingTags.size()))
                    .divide(BigDecimal.valueOf(tags.size()), RoundingMode.CEILING)
                    .intValue(),
                postTitleQueryRecords.size()
            );

            LOGGER.info("Fetch post titles using a tons of queries");

            //The Spring Data findAll Anti-Pattern
            List<String> postTitlesStreamRecords = postRepository.findAll()
                .stream()
                .filter(
                    post -> post.getTags()
                        .stream()
                        .map(Tag::getName)
                        .anyMatch(matchingTags::contains)
                )
                .sorted(Comparator.comparing(Post::getId))
                .map(Post::getTitle)
                .collect(Collectors.toList());

            assertEquals(postTitleQueryRecords, postTitlesStreamRecords);

            return null;
        });
    }

    @Test
    public void testUpdate() {
        Post post = forumService.createPost(
            1L,
            "High-Performance Java Persistence"
        );

        forumService.updatePostTitle(
            1L,
            "High-Performance Java Persistence 2nd edition"
        );

        assertEquals(
            "High-Performance Java Persistence 2nd edition",
            forumService.findById(1L).getTitle()
        );
    }

    @Test
    public void testDeleteAll() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                entityManager.persist(
                    new Post()
                        .setId(1L)
                        .setTitle("High-Performance Java Persistence")
                        .addComment(
                            new PostComment()
                                .setId(1L)
                                .setReview("Best book on JPA and Hibernate!")
                        )
                        .addComment(
                            new PostComment()
                                .setId(2L)
                                .setReview("A must-read for every Java developer!")
                        )
                );

                entityManager.persist(
                    new Post()
                        .setId(2L)
                        .setTitle("Hypersistence Optimizer")
                        .addComment(
                            new PostComment()
                                .setId(3L)
                                .setReview("It's like pair programming with Vlad!")
                        )
                );
                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }

        forumService.deleteAll();
    }
}

