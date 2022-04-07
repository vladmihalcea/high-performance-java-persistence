package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;

/**
 * @author Vlad Mihalcea
 */
public class EagerFetchingCollectionsFindVsQueryTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class,
                Tag.class
        };
    }
    @Override
    public void afterInit() {
        doInJPA(entityManager -> {

            List<Tag> tags = new ArrayList<>();

            for (long i = 1; i <= 3; i++) {
                Tag tag = new Tag()
                    .setId(i)
                    .setName(String.format("Tag nr. %d", i + 1));

                entityManager.persist(tag);
                tags.add(tag);
            }

            long commentId = 0;

            Post post = new Post()
                .setId(1L)
                .setTitle(String.format("Post nr. %d", 1L));


            for (long i = 0; i < 2; i++) {
                post.addComment(
                    new PostComment()
                        .setId(++commentId)
                        .setReview("Excellent!")
                );
            }

            for (int i = 0; i < 3; i++) {
                post.getTags().add(tags.get(i));
            }

            entityManager.persist(post);
        });
    }

    @Test
    public void testFindEntityById() {
        doInJPA(entityManager -> {
            Post post =  entityManager.find(Post.class, 1L);

            assertFalse(post.getComments().isEmpty());
            assertFalse(post.getTags().isEmpty());
        });
    }

    @Test
    public void testQueryEntityById() {
        doInJPA(entityManager -> {
            Post post = entityManager.createQuery(
                "select p from Post p where p.id = 1L", Post.class)
            .getSingleResult();

            assertFalse(post.getComments().isEmpty());
            assertFalse(post.getTags().isEmpty());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(
            mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
        )
        private Set<PostComment> comments = new HashSet<>();

        @ManyToMany(
            cascade = {
                CascadeType.PERSIST,
                CascadeType.MERGE
            },
            fetch = FetchType.EAGER
        )
        @JoinTable(name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
        )
        private Set<Tag> tags = new HashSet<>();

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }

        public Set<PostComment> getComments() {
            return comments;
        }

        public Post addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
            return this;
        }

        public Set<Tag> getTags() {
            return tags;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }
    }

    @Entity(name = "Tag")
    @Table(name = "tag")
    public static class Tag {

        @Id
        private Long id;

        private String name;

        public Long getId() {
            return id;
        }

        public Tag setId(Long id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public Tag setName(String name) {
            this.name = name;
            return this;
        }
    }
}
