package com.vladmihalcea.book.hpjp.hibernate.fetching.file;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLQueryToFileTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        //properties.setProperty(AvailableSettings.USE_STREAMS_FOR_BINARY, Boolean.FALSE.toString());
        return properties;
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class,
        };
    }


    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            for (long id = 1; id <= 100; id++) {
                Post post = new Post();
                post.setId(id);
                post.setTitle("High-Performance Java Persistence");

                post.addComment(new PostComment("Excellent!"));
                post.addComment(new PostComment("Great!"));

                entityManager.persist(post);
            }
        });
    }

    @Test
    public void testCopy() throws URISyntaxException {
        doInJPA(entityManager -> {
            entityManager.createNativeQuery(
                String.format(
                    "copy (" +
                    "   select * " +
                    "   from post p " +
                    "   inner join post_comment pc on pc.post_id = p.id " +
                    ") " +
                    "to '%s' " +
                    "with CSV DELIMITER ','",
                    Paths.get(System.getenv("PGSQL_DATA"), "post_and_comments.csv").toString()
                )
            )
            .executeUpdate();
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true)
        private List<PostComment> comments = new ArrayList<>();

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

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }

        @Override
        public String toString() {
            return "Post{" +
                    "id=" + id +
                    ", title='" + title + '\'' +
                    '}';
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        public PostComment() {}

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
}
