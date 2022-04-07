package com.vladmihalcea.book.hpjp.hibernate.mapping;

import com.vladmihalcea.book.hpjp.hibernate.forum.MediaType;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Vlad Mihalcea
 */
public class OptionalAttributeTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class,
            Attachment.class
        };
    }

    @Test
    public void testLifecycle() {
        Attachment notAvailable = new Attachment();
        notAvailable.setContent(new byte[] {-1});

        doInJPA(entityManager -> {
            byte[] coverContent = new byte[] {1, 2, 3};

            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);

            PostComment comment1 = new PostComment();
            comment1.setPost(post);

            entityManager.persist(comment1);

            Attachment cover = new Attachment();
            cover.setContent(coverContent);
            entityManager.persist(cover);

            PostComment comment2 = new PostComment();
            comment2.setPost(post);
            comment2.setAttachment(cover);

            entityManager.persist(comment2);
        });
        doInJPA(entityManager -> {
            List<PostComment> comments = entityManager.createQuery(
                "select pc " +
                "from PostComment pc " +
                "join pc.post p " +
                "where p.id = :postId", PostComment.class)
            .setParameter("postId", 1L)
            .getResultList();

            List<Attachment> attachments =
                comments.stream()
                .map(pc -> pc.getAttachment()
                .orElse(notAvailable))
                .collect(Collectors.toList());

            attachments.forEach(attachment -> LOGGER.info("Attachment content {}", attachment.getContent()));
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post implements Serializable {

        @Id
        private Long id;

        private String title;

        public Post() {}

        public Post(String title) {
            this.title = title;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment implements Serializable {

        @Id
        @GeneratedValue
        private Long id;

        private String review;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        @ManyToOne(fetch = FetchType.LAZY)
        private Attachment attachment;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public Optional<Attachment> getAttachment() {
            return Optional.ofNullable(attachment);
        }

        public void setAttachment(Attachment attachment) {
            this.attachment = attachment;
        }
    }

    @MappedSuperclass
    public static class BaseAttachment {

        @Id
        @GeneratedValue
        private Long id;

        private String name;

        @Enumerated
        @Column(name = "media_type")
        private MediaType mediaType;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public MediaType getMediaType() {
            return mediaType;
        }

        public void setMediaType(MediaType mediaType) {
            this.mediaType = mediaType;
        }
    }

    @Entity(name = "Attachment")
    @Table(name = "attachment")
    public static class Attachment extends BaseAttachment {

        @Lob
        private byte[] content;

        public byte[] getContent() {
            return content;
        }

        public void setContent(byte[] content) {
            this.content = content;
        }
    }
}
