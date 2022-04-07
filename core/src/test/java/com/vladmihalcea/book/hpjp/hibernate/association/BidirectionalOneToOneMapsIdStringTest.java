package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class BidirectionalOneToOneMapsIdStringTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostDetails.class,
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Post post = new Post("First post");
            post.setId("ABC12");

            PostDetails details = new PostDetails();
            post.setDetails(details);

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            LOGGER.info("Fetching Post");
            Post post = entityManager.find(Post.class, "ABC12");
        });

        doInJPA(entityManager -> {
            LOGGER.info("Fetching Post");
            PostDetails details = entityManager.find(PostDetails.class, "ABC12");
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @Column(name = "post_id")
        private String id;

        private String title;

        @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private PostDetails details;

        public Post() {}

        public Post(String title) {
            this.title = title;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public PostDetails getDetails() {
            return details;
        }

        public void setDetails(PostDetails details) {
            if (details == null) {
                if (this.details != null) {
                    this.details.setPost(null);
                }
            }
            else {
                details.setPost(this);
            }
            this.details = details;
        }
    }

    @Entity(name = "PostDetails")
    @Table(name = "post_details")
    public static class PostDetails {

        @Id
        //@Column(name = "post_id")
        private String id;

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        @JoinColumn(name = "post_id")
        private Post post;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }
    }
}
