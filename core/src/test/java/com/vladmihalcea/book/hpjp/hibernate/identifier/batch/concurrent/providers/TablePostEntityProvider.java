package com.vladmihalcea.book.hpjp.hibernate.identifier.batch.concurrent.providers;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
public class TablePostEntityProvider extends PostEntityProvider<TablePostEntityProvider.Post> {

    public TablePostEntityProvider() {
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
        @GenericGenerator(name = "table", strategy = "enhanced-table", parameters = {
                @org.hibernate.annotations.Parameter(name = "table_name", value = "sequence_table"),
                @org.hibernate.annotations.Parameter(name = "increment_size", value = "100"),
                @org.hibernate.annotations.Parameter(name = "optimizer", value = "pooled"),
        })
        @GeneratedValue(generator = "table", strategy=GenerationType.TABLE)
        private Long id;
    }
}
