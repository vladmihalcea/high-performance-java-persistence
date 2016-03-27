package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertNotNull;

/**
 * <code>FindEntityTest</code> - Find entity Test
 *
 * @author Vlad Mihalcea
 */
public class EagerFetchingOneToManyFindEntityTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class,
                Tag.class
        };
    }


    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle(String.format("Post nr. %d", 1));
            PostComment comment = new PostComment();
            comment.setId(1L);
            comment.setPost(post);
            comment.setReview("Excellent!");
            entityManager.persist(post);
            entityManager.persist(comment);
        });
    }

    @Test
    public void testGet() {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertNotNull(post);
        });
    }

    @Test
    public void testFindWithQuery() {
        doInJPA(entityManager -> {
            Long postId =  1L;
            Post post = entityManager.createQuery(
                    "select p from Post p where p.id = :id", Post.class)
                    .setParameter("id", postId)
                    .getSingleResult();
            assertNotNull(post);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(mappedBy = "post", fetch = FetchType.EAGER)
        private Set<PostComment> comments = new HashSet<>();

        @ManyToMany(fetch = FetchType.EAGER)
        @JoinTable(name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
        )
        private Set<Tag> tags = new HashSet<>();

        public Post() {
        }

        public Post(Long id) {
            this.id = id;
        }

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

        public Set<Tag> getTags() {
            return tags;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne
        private Post post;

        private String review;

        public PostComment() {
        }

        public PostComment(String review) {
            this.review = review;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
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

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
