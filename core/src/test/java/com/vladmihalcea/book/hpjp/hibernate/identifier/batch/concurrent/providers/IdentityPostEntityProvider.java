package com.vladmihalcea.book.hpjp.hibernate.identifier.batch.concurrent.providers;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
public class IdentityPostEntityProvider extends PostEntityProvider<IdentityPostEntityProvider.Post> {

    public IdentityPostEntityProvider() {
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
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
    }
}
