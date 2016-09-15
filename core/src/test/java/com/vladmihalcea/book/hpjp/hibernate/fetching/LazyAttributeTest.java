package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.hibernate.event.Image;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Vlad Mihalcea
 */
public class LazyAttributeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Image.class,
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Image image = new Image();
            image.setId(1L);
            image.setContent(new byte[] {1, 2, 3});
            entityManager.persist(image);
        });
        doInJPA(entityManager -> {
            Image image = entityManager.find(Image.class, 1L);
            LOGGER.debug("Fetched image");
            assertArrayEquals(new byte[] {1, 2, 3}, image.getContent());
        });
    }
}
