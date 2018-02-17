package com.vladmihalcea.book.hpjp.hibernate.bytecode;

import com.vladmihalcea.book.hpjp.hibernate.forum.Tag;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

/**
 * @author Vlad Mihalcea
 */
public class DirtyCheckingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Tag.class
        };
    }

    @Test
    public void testDirtyChecking() {
        doInJPA(entityManager -> {
            Tag tag = new Tag();
            tag.setId(1L);
            tag.setName("High-Performance Hibernate");

            entityManager.persist(tag);
        });

        doInJPA(entityManager -> {
            Tag tag = entityManager.find(Tag.class, 1L);

            tag.setName("High-Performance Java Persistence");
            entityManager.flush();
        });
    }
}
