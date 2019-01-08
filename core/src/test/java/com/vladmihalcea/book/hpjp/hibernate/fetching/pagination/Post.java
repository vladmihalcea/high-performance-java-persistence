package com.vladmihalcea.book.hpjp.hibernate.fetching.pagination;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;

import javax.persistence.*;
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
        query =
                "SELECT * " +
                        "FROM (   " +
                        "    SELECT *, dense_rank() OVER (ORDER BY \"pc.post_id\") rank " +
                        "    FROM (   " +
                        "        SELECT p.id AS \"p.id\", " +
                        "               p.created_on AS \"p.created_on\", " +
                        "               p.title AS \"p.title\", " +
                        "               pc.id as \"pc.id\", " +
                        "               pc.created_on AS \"pc.created_on\", " +
                        "               pc.review AS \"pc.review\", " +
                        "               pc.post_id AS \"pc.post_id\" " +
                        "        FROM post p  " +
                        "        LEFT JOIN post_comment pc ON p.id = pc.post_id  " +
                        "        ORDER BY p.created_on " +
                        "    ) p_pc " +
                        ") p_pc_r " +
                        "WHERE p_pc_r.rank <= :rank",
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

    @Column(name = "created_on")
    private Timestamp createdOn;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
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

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
    }

    public List<PostComment> getComments() {
        return comments;
    }

    public void setComments(List<PostComment> comments) {
        this.comments = comments;
    }

    public void addComment(PostComment comment) {
        comments.add(comment);
        comment.setPost(this);
    }
}
