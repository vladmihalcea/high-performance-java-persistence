package com.vladmihalcea.hpjp.spring.data.cascade;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.cascade.config.SpringDataJPACascadeConfiguration;
import com.vladmihalcea.hpjp.spring.data.cascade.domain.Post;
import com.vladmihalcea.hpjp.spring.data.cascade.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.cascade.domain.PostDetails;
import com.vladmihalcea.hpjp.spring.data.cascade.domain.Tag;
import com.vladmihalcea.hpjp.spring.data.cascade.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.cascade.repository.PostDetailsRepository;
import com.vladmihalcea.hpjp.spring.data.cascade.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.data.cascade.repository.TagRepository;
import org.hibernate.Session;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionCallback;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPACascadeConfiguration.class)
public class SpringDataJPACascadeTest extends AbstractSpringTest {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Autowired
    private PostDetailsRepository postDetailsRepository;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            PostComment.class,
            PostDetails.class,
            Post.class,
            Tag.class
        };
    }

    @Test
    public void testSavePostAndComments() {
        postRepository.persist(
            new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence")
                .addComment(
                    new PostComment()
                        .setReview("Best book on JPA and Hibernate!")
                )
                .addComment(
                    new PostComment()
                        .setReview("A must-read for every Java developer!")
                )
        );

        transactionTemplate.execute(transactionStatus -> {
            Post post = postRepository.findByIdWithComments(1L);
            postRepository.delete(post);

            return null;
        });
    }

    @Test
    public void testSavePostWithDetailsAndComments() {
        Post post = new Post()
            .setId(1L)
            .setTitle("High-Performance Java Persistence")
            .setDetails(
                new PostDetails()
                    .setCreatedBy("Vlad Mihalcea")
            )
            .addComment(
                new PostComment()
                    .setReview("Best book on JPA and Hibernate!")
            )
            .addComment(
                new PostComment()
                    .setReview("A must-read for every Java developer!")
            );

        List<Long> postCommentIds = transactionTemplate.execute(transactionStatus -> {
            postRepository.persist(post);
            return post.getComments().stream().map(PostComment::getId).toList();
        });

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Long minId = Collections.min(postCommentIds);
            Long maxId = Collections.max(postCommentIds);

            List<PostComment> postComments = postCommentRepository.findAllWithPostAndDetailsByIds(
                minId,
                maxId
            );

            assertEquals(postCommentIds.size(), postComments.size());
            return null;
        });
    }

    @Test
    public void testSavePostAndPostDetails() {
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence")
                .setDetails(
                    new PostDetails()
                        .setCreatedBy("Vlad Mihalcea")
                );

            postRepository.persist(post);
            return null;
        });

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Post post = postRepository.getReferenceById(1L);

            PostDetails postDetails = postDetailsRepository.findById(post.getId()).orElseThrow();
            assertEquals("Vlad Mihalcea", postDetails.getCreatedBy());

            return null;
        });

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Post post = postRepository.findById(1L).orElseThrow();

            return null;
        });
    }

    @Test
    public void testSavePostAndTags() {
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            tagRepository.persist(new Tag().setName("JPA"));
            tagRepository.persist(new Tag().setName("Hibernate"));

            return null;
        });

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Session session = entityManager.unwrap(Session.class);

            postRepository.persist(
                new Post()
                    .setId(1L)
                    .setTitle("JPA with Hibernate")
                    .addTag(session.bySimpleNaturalId(Tag.class).getReference("JPA"))
                    .addTag(session.bySimpleNaturalId(Tag.class).getReference("Hibernate"))
            );

            postRepository.persist(
                new Post()
                    .setId(2L)
                    .addTag(session.bySimpleNaturalId(Tag.class).getReference("Hibernate"))
            );

            return null;
        });

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Session session = entityManager.unwrap(Session.class);

            Post post = entityManager.createQuery("""
                select p
                from Post p
                join fetch p.tags
                where p.id = :id
                """, Post.class)
            .setParameter("id", 1L)
            .getSingleResult();

            post.getTags().remove(session.bySimpleNaturalId(Tag.class).getReference("JPA"));

            return null;
        });
    }

    @Test
    public void testBatchingPersistPostAndComments() {
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            for (long i = 1; i <= 3; i++) {
                postRepository.persist(
                    new Post()
                        .setId(i)
                        .setTitle(String.format("Post no. %d", i))
                        .addComment(new PostComment().setReview("Good"))
                );
            }
            return null;
        });
    }

    @Test
    public void testBatchingUpdatePost() {
        testBatchingPersistPostAndComments();

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            List<Post> posts = postRepository.findAllByTitleLike("Post no.%");

            posts.forEach(post -> post.setTitle(post.getTitle().replaceAll("no", "nr")));
            return null;
        });
    }
    
    @Test
    public void testBatchingUpdatePostAndComments() {
        testBatchingPersistPostAndComments();

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            List<PostComment> comments = postCommentRepository.findAllWithPostTitleLike("Post no.%");

            comments.forEach(c -> {
                c.setReview(c.getReview().replaceAll("Good", "Very good"));
                Post post = c.getPost();
                post.setTitle(post.getTitle().replaceAll("no", "nr"));
            });
            return null;
        });
    }

    @Test
    public void testBatchingDeletePost() {
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            for (long i = 1; i <= 3; i++) {
                postRepository.persist(
                    new Post()
                        .setId(i)
                        .setTitle(String.format("Post no. %d", i))
                );
            }
            return null;
        });

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            List<Post> posts = postRepository.findAllByTitleLike("Post no.%");

            posts.forEach(post -> postRepository.delete(post));
            return null;
        });
    }

    @Test
    public void testBatchingDeletePostAndComments() {
        testBatchingPersistPostAndComments();

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            List<Post> posts = postRepository.findAllByTitleLike("Post no.%");

            posts.forEach(postRepository::delete);
            return null;
        });
    }

    @Test
    public void testBatchingDeletePostAndCommentsManualOrdering() {
        testBatchingPersistPostAndComments();

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            List<Post> posts = postRepository.findAllByTitleLike("Post no.%");

            posts.forEach(post -> {
                Iterator<PostComment> it = post.getComments().iterator();
                while (it.hasNext()) {
                    it.next().setPost(null);
                    it.remove();
                }
            });

            entityManager.flush();

            posts.forEach(postRepository::delete);

            return null;
        });
    }

    @Test
    @Ignore("""
        Requires the comments collection to use
        @OneToMany(mappedBy = \"post\", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
        """)
    public void testBatchingDeletePostAndCommentsBulkDelete() {
        testBatchingPersistPostAndComments();

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            List<Post> posts = postRepository.findAllByTitleLike("Post no.%");

            postCommentRepository.deleteAllByPost(posts);
            posts.forEach(postRepository::delete);

            return null;
        });
    }
}

