package com.vladmihalcea.book.hpjp.hibernate.query;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class JoinUnrelatedEntitiesTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PageView.class
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setSlug("/books/high-performance-java-persistence");
            post.setTitle("High-Performance Java Persistence");

            entityManager.persist(post);
        });
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setSlug("/presentations");
            post.setTitle("Presentations");

            entityManager.persist(post);
        });
        doInJPA(entityManager -> {
            PageView pageView = new PageView();
            pageView.setSlug("/books/high-performance-java-persistence");
            pageView.setIpAddress("127.0.0.1");

            entityManager.persist(pageView);
        });
        doInJPA(entityManager -> {
            PageView pageView = new PageView();
            pageView.setSlug("/books/high-performance-java-persistence");
            pageView.setIpAddress("192.168.0.1");

            entityManager.persist(pageView);
        });
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Tuple postViewCount = entityManager.createQuery(
                "select p as post, count(pv) as page_views " +
                "from Post p " +
                "left join PageView pv on p.slug = pv.slug " +
                "where p.title = :title " +
                "group by p", Tuple.class)
            .setParameter("title", "High-Performance Java Persistence")
            .getSingleResult();

            Post post = (Post) postViewCount.get("post");
            assertEquals("/books/high-performance-java-persistence", post.getSlug());

            int pageViews = ((Number) postViewCount.get("page_views")).intValue();
            assertEquals(2, pageViews);
        });

        doInJPA(entityManager -> {
            Tuple postViewCount = entityManager.createQuery(
                "select p as post, count(pv) as page_views " +
                "from Post p " +
                "left join PageView pv on p.slug = pv.slug " +
                "where p.title = :title " +
                "group by p", Tuple.class)
            .setParameter("title", "Presentations")
            .getSingleResult();

            Post post = (Post) postViewCount.get("post");
            assertEquals("/presentations", post.getSlug());

            int pageViews = ((Number) postViewCount.get("page_views")).intValue();
            assertEquals(0, pageViews);
        });

        doInJPA(entityManager -> {
            Tuple postViewCount = entityManager.createQuery(
                "select p as post, count(pv) as page_views " +
                "from Post p, PageView pv " +
                "where p.title = :title and " +
                "      ( pv is null or p.slug = pv.slug ) " +
                "group by p", Tuple.class)
            .setParameter("title", "High-Performance Java Persistence")
            .getSingleResult();

            Post post = (Post) postViewCount.get("post");
            assertEquals("/books/high-performance-java-persistence", post.getSlug());

            int pageViews = ((Number) postViewCount.get("page_views")).intValue();
            assertEquals(2, pageViews);
        });

        doInJPA(entityManager -> {
            List<Tuple> postViewCount = entityManager.createQuery(
                "select p as post, count(pv) as page_views " +
                "from Post p, PageView pv " +
                "where p.title = :title and " +
                "      ( p.slug = pv.slug ) " +
                "group by p", Tuple.class)
            .setParameter("title", "Presentations")
            .getResultList();

            assertEquals(0, postViewCount.size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String slug;

        private String title;

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

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }
    }

    @Entity(name = "PageView")
    @Table(name = "page_view")
    public static class PageView {

        @Id
        @GeneratedValue
        private Long id;

        private String slug;

        @CreationTimestamp
        @Column(name = "created_on")
        private Date createdOn;

        @Column(name = "ip_address")
        private String ipAddress;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }
    }

}
