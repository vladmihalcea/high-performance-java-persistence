package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.hibernate.forum.Attachment;
import com.vladmihalcea.book.hpjp.hibernate.forum.MediaType;
import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class LazyAttributeTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Attachment.class,
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        //properties.setProperty(AvailableSettings.USE_STREAMS_FOR_BINARY, Boolean.FALSE.toString());
        return properties;
    }

    @Test
    public void test() throws URISyntaxException {
        final Path bookFilePath = Paths.get(Thread.currentThread().getContextClassLoader().getResource("ehcache.xml").toURI());
        final Path videoFilePath = Paths.get(Thread.currentThread().getContextClassLoader().getResource("spy.properties").toURI());

        doInJPA(entityManager -> {
            try {
                entityManager.persist(
                    new Attachment()
                        .setId(1L)
                        .setName("High-Performance Java Persistence")
                        .setMediaType(MediaType.PDF)
                        .setContent(Files.readAllBytes(bookFilePath))
                );

                entityManager.persist(
                    new Attachment()
                        .setId(2L)
                        .setName("High-Performance Java Persistence - Mach 2")
                        .setMediaType(MediaType.MPEG_VIDEO)
                        .setContent(Files.readAllBytes(videoFilePath))
                );
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });

        doInJPA(entityManager -> {
            try {
                Attachment book = entityManager.find(Attachment.class, 1L);
                LOGGER.debug("Fetched book: {}", book.getName());
                assertArrayEquals(Files.readAllBytes(bookFilePath), book.getContent());

                Attachment video = entityManager.find(Attachment.class, 2L);
                LOGGER.debug("Fetched video: {}", video.getName());
                assertArrayEquals(Files.readAllBytes(videoFilePath), video.getContent());
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });
    }
}
