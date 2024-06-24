package com.vladmihalcea.hpjp.hibernate.mapping.enums;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.ReflectionUtils;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JavaType;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class EnumCustomOrdinalTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void afterInit() {
        executeStatement("ALTER TABLE post DROP COLUMN status");
        executeStatement("ALTER TABLE post ADD COLUMN status NUMERIC(3)");
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1)
                    .setTitle("To be moderated")
                    .setStatus(PostStatus.REQUIRES_MODERATOR_INTERVENTION)
            );
            entityManager.persist(
                new Post()
                    .setId(2)
                    .setTitle("Pending")
                    .setStatus(PostStatus.PENDING)
            );
            entityManager.persist(
                new Post()
                    .setId(3)
                    .setTitle("Approved")
                    .setStatus(PostStatus.APPROVED)
            );
            entityManager.persist(
                new Post()
                    .setId(4)
                    .setTitle("Spam post")
                    .setStatus(PostStatus.SPAM)
            );
        });

        doInJPA(entityManager -> {
            assertEquals(
                PostStatus.REQUIRES_MODERATOR_INTERVENTION,
                entityManager.find(Post.class, 1).getStatus()
            );
            assertEquals(
                PostStatus.PENDING,
                entityManager.find(Post.class, 2).getStatus()
            );
            assertEquals(
                PostStatus.APPROVED,
                entityManager.find(Post.class, 3).getStatus()
            );
            assertEquals(
                PostStatus.SPAM,
                entityManager.find(Post.class, 4).getStatus()
            );
        });
    }

    public enum PostStatus {
        PENDING(100),
        APPROVED(10),
        SPAM(50),
        REQUIRES_MODERATOR_INTERVENTION(1);

        private final int statusCode;

        PostStatus(int statusCode) {
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Integer id;

        @Column(length = 250)
        private String title;

        @Column(columnDefinition = "NUMERIC(3)")
        @JavaType(PostStatusJavaType.class)
        private PostStatus status;

        public Integer getId() {
            return id;
        }

        public Post setId(Integer id) {
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

        public PostStatus getStatus() {
            return status;
        }

        public Post setStatus(PostStatus status) {
            this.status = status;
            return this;
        }
    }

    public static abstract class CustomOrdinalValueEnumJavaType<T extends Enum<T>> extends EnumJavaType<T> {

        private Map<T, Integer> enumToCustomOrdinalValueMap = new HashMap<>();
        private Map<Integer, T> customOrdinalValueToEnumMap = new HashMap<>();

        public CustomOrdinalValueEnumJavaType(Class<T> type) {
            super(type);
            T[] enumValues = ReflectionUtils.invokeStaticMethod(
                ReflectionUtils.getMethod(type, "values")
            );
            for(T enumValue : enumValues) {
                Integer customOrdinalValue = getCustomOrdinalValue(enumValue);
                enumToCustomOrdinalValueMap.put(enumValue, customOrdinalValue);
                customOrdinalValueToEnumMap.put(customOrdinalValue, enumValue);
            }
        }

        protected abstract Integer getCustomOrdinalValue(T enumValue);

        public Byte toByte(T domainForm) {
            return domainForm != null ?
                enumToCustomOrdinalValueMap.get(domainForm).byteValue() : null
            ;
        }

        public Short toShort(T domainForm) {
            return domainForm != null ?
                enumToCustomOrdinalValueMap.get(domainForm).shortValue() : null
            ;
        }

        public Integer toInteger(T domainForm) {
            return domainForm != null ?
                enumToCustomOrdinalValueMap.get(domainForm) : null
            ;
        }

        public Long toLong(T domainForm) {
            return domainForm != null ?
                enumToCustomOrdinalValueMap.get(domainForm).longValue() : null
            ;
        }

        public T fromByte(Byte byteValue) {
            return byteValue != null ?
                customOrdinalValueToEnumMap.get(byteValue.intValue()) : null
            ;
        }
    }

    public static class PostStatusJavaType extends CustomOrdinalValueEnumJavaType<PostStatus> {
        public PostStatusJavaType() {
            super(PostStatus.class);
        }

        @Override
        protected Integer getCustomOrdinalValue(PostStatus postStatus) {
            return postStatus.getStatusCode();
        }
    }
}
