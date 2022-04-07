package com.vladmihalcea.book.hpjp.hibernate.identifier;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.RowId;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractOracleIntegrationTest;

import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class OracleRowIdTest extends AbstractOracleIntegrationTest {
    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class
        };
    }

    @Test
    public void test() {
        doInJPA( entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");

            entityManager.persist(post);

            PostComment comment1 = new PostComment();
            comment1.setReview("Great!");
            post.addComment(comment1);

            PostComment comment2 = new PostComment();
            comment2.setReview("To read");
            post.addComment(comment2);

            PostComment comment3 = new PostComment();
            comment3.setReview("Lorem Ipsum");
            post.addComment(comment3);
        } );

        Post _post = doInJPA( entityManager -> {
            return entityManager.createQuery(
                "select p " +
                "from Post p " +
                "join fetch p.comments " +
                "where p.id = :id", Post.class)
            .setParameter( "id", 1L )
            .getSingleResult();
        } );

        List<PostComment>_comments = _post.getComments();

        _post.getComments().get( 0 ).setReview( "Must read!" );
        _post.removeComment( _comments.get( 2 ) );

        doInJPA( entityManager -> {
            entityManager.merge( _post );
        } );
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @RowId( "ROWID" )
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

        public List<PostComment> getComments() {
            return comments;
        }

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }

        public void removeComment(PostComment comment) {
            comments.remove(comment);
            comment.setPost(null);
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    @RowId( "ROWID" )
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PostComment)) return false;
            return id != null && id.equals(((PostComment) o).getId());
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }
}
