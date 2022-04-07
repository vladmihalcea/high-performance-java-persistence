package com.vladmihalcea.book.hpjp.hibernate.fetching.pagination;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Post")
@Table(name = "post")
@NamedNativeQuery(
    name = "PostWithCommentByRank",
    query = """
        SELECT *
        FROM (
            SELECT
                *,
                DENSE_RANK() OVER (
                    ORDER BY "p.created_on", "p.id"
                ) rank
            FROM (
                SELECT
                    p.id AS "p.id", p.created_on AS "p.created_on",
                    p.title AS "p.title", pc.post_id AS "pc.post_id",
                    pc.id as "pc.id", pc.created_on AS "pc.created_on",
                    pc.review AS "pc.review"
                FROM  post p
                LEFT JOIN post_comment pc ON p.id = pc.post_id
                WHERE p.title LIKE :titlePattern
                ORDER BY p.created_on
            ) p_pc
        ) p_pc_r
        WHERE p_pc_r.rank <= :rank
        """,
    resultSetMapping = "PostWithCommentByRankMapping"
)
@SqlResultSetMapping(
    name = "PostWithCommentByRankMapping",
    entities = {
        @EntityResult(
            entityClass = Post.class,
            fields = {
                @FieldResult(name = "id", column = "p.id"),
                @FieldResult(name = "createdOn", column = "p.created_on"),
                @FieldResult(name = "title", column = "p.title"),
            }
        ),
        @EntityResult(
            entityClass = PostComment.class,
            fields = {
                @FieldResult(name = "id", column = "pc.id"),
                @FieldResult(name = "createdOn", column = "pc.created_on"),
                @FieldResult(name = "review", column = "pc.review"),
                @FieldResult(name = "post", column = "pc.post_id"),
            }
        )
    }
)
public class Post implements Identifiable<Long> {

    @Id
    private Long id;

    private String title;

    @Column(name = "created_on", nullable = false)
    private Timestamp createdOn;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostComment> comments = new ArrayList<>();

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

    public Date getCreatedOn() {
        return createdOn;
    }

    public Post setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    public List<PostComment> getComments() {
        return comments;
    }

    public Post setComments(List<PostComment> comments) {
        this.comments = comments;
        return this;
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
