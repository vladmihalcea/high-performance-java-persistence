package com.vladmihalcea.hpjp.hibernate.bytecode;

import com.vladmihalcea.hpjp.hibernate.forum.Tag;
import com.vladmihalcea.hpjp.util.AbstractTest;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;

/**
 * @author Vlad Mihalcea
 */
@BytecodeEnhanced
public class BytecodeEnhancementDirtyCheckingTest extends AbstractTest {

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
