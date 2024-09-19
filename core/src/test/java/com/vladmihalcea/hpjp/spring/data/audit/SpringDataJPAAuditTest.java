package com.vladmihalcea.hpjp.spring.data.audit;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.audit.config.SpringDataJPAAuditConfiguration;
import com.vladmihalcea.hpjp.spring.data.audit.domain.Post;
import com.vladmihalcea.hpjp.spring.data.audit.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.audit.domain.PostStatus;
import com.vladmihalcea.hpjp.spring.data.audit.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.data.audit.service.PostService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.history.Revision;
import org.springframework.data.history.RevisionSort;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPAAuditConfiguration.class)
public class SpringDataJPAAuditTest extends AbstractSpringTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostService postService;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class,
            PostComment.class
        };
    }

    @Test
    public void test() {
        Post post = new Post()
            .setTitle("High-Performance Java Persistence 1st edition")
            .setSlug("high-performance-java-persistence")
            .setStatus(PostStatus.APPROVED);

        postService.savePostAndComments(
            post,
            new PostComment()
                .setPost(post)
                .setReview("A must-read for every Java developer!"),
            new PostComment()
                .setPost(post)
                .setReview("Best book on JPA and Hibernate!")
        );

        post.setTitle("High-Performance Java Persistence 2nd edition");
        postService.savePost(post);

        postService.deletePost(post);

        Revision<Long, Post> latestRevision = postRepository
            .findLastChangeRevision(post.getId())
            .orElseThrow();

        LOGGER.info(
            "The latest Post entity operation was [{}] at revision [{}]",
            latestRevision.getMetadata().getRevisionType(),
            latestRevision.getRevisionNumber().orElseThrow()
        );

        for(Revision<Long, Post> revision : postRepository.findRevisions(post.getId())) {
            LOGGER.info(
                "At revision [{}], the Post entity state was: [{}]",
                revision.getRevisionNumber().orElseThrow(),
                revision.getEntity()
            );
        }

        testPagination();
    }

    private void testPagination() {
        Post post = new Post()
            .setTitle("Hypersistence Optimizer, version 1.0.0")
            .setSlug("hypersistence-optimizer")
            .setStatus(PostStatus.APPROVED);
        postService.savePost(post);

        for (int i = 1; i < 20; i++) {
            post.setTitle(
                String.format(
                    "Hypersistence Optimizer, version 1.%d.%d",
                    i/10,
                    i%10
                )
            );
            postService.savePost(post);
        }

        int pageSize = 10;

        Page<Revision<Long, Post>> firstPage = postRepository.findRevisions(
            post.getId(),
            PageRequest.of(0, pageSize, RevisionSort.desc())
        );

        logPage(firstPage);

        Page<Revision<Long, Post>> secondPage = postRepository.findRevisions(
            post.getId(),
            PageRequest.of(1, pageSize, RevisionSort.desc())
        );

        logPage(secondPage);
    }

    private void logPage(Page<Revision<Long, Post>> revisionPage) {
        for(Revision<Long, Post> revision : revisionPage) {
            LOGGER.info(
                String.format(
                    "At revision [%02d], the Post entity state was: [%s]",
                    revision.getRevisionNumber().orElseThrow(),
                    revision.getEntity()
                )
            );
        }
    }
}

