package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ElementCollectionNestedTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Post post = new Post("First post");

            PostInfo postInfo1 = new PostInfo();
            postInfo1.title = "1";
            /*postInfo1.moreInfos.add("1.1");
            postInfo1.moreInfos.add("1.2");*/

            PostInfo postInfo2 = new PostInfo();
            postInfo2.title = "2";

            /*postInfo1.moreInfos.add("2.1");
            postInfo1.moreInfos.add("3.2");*/

            post.infos.add(postInfo1);
            post.infos.add(postInfo2);

            entityManager.persist(post);
        });
        doInJPA(entityManager -> {
            Post post = entityManager.createQuery("from Post", Post.class).getSingleResult();
            assertEquals(2, post.infos.size());
            //assertEquals(2, post.infos.get(0).moreInfos.size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        public Post() {}

        public Post(String title) {
            this.title = title;
        }

        @ElementCollection
        private List<PostInfo> infos = new ArrayList<>();

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

        public List<PostInfo> getInfos() {
            return infos;
        }
    }

    @Embeddable
    public static class PostInfo {

        private String title;

        /*@ElementCollection
        private List<String> moreInfos = new ArrayList<>();*/
    }
}
