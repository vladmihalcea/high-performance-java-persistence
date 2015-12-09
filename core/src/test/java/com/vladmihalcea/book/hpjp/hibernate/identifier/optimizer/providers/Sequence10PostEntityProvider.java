package com.vladmihalcea.book.hpjp.hibernate.identifier.optimizer.providers;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * <code>Sequence10PostEntityProvider</code> - Sequence 10 PostEntityProvider
 *
 * @author Vlad Mihalcea
 */
public class Sequence10PostEntityProvider extends PostEntityProvider<Sequence10PostEntityProvider.Post> {

    public Sequence10PostEntityProvider() {
        super(Post.class);
    }

    @Override
    public Post newPost() {
        return new Post();
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GenericGenerator(name = "table", strategy = "enhanced-sequence", parameters = {
                @org.hibernate.annotations.Parameter(name = "table_name", value = "sequence_table"),
                @org.hibernate.annotations.Parameter(name = "increment_size", value = "10"),
                @org.hibernate.annotations.Parameter(name = "optimizer", value = "pooled"),
        })
        @GeneratedValue(generator = "table", strategy=GenerationType.TABLE)
        private Long id;
    }
}
