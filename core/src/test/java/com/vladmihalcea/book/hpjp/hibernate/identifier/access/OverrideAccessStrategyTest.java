package com.vladmihalcea.book.hpjp.hibernate.identifier.access;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
public class OverrideAccessStrategyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            FieldEntity.class
        };
    }

    @Test
    public void testSequenceIdentifierGenerator() {
        doInJPA(entityManager -> {
            FieldEntity entity = new FieldEntity();
            entity.setId(1);
            entityManager.persist(entity);
        });
        doInJPA(entityManager -> {
            FieldEntity entity = entityManager.find(FieldEntity.class, 1);
            entity.setName("abc");
        });
    }

    @Entity(name = "FieldEntity")
    public static class FieldEntity {

        private Integer id;

        private String name;

        @Version
        @Access(AccessType.FIELD)
        private Long version;

        @Id
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
