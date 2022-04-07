package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.hibernate.forum.MediaType;
import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class LazyAttributeWithMultipleEntitiesTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Attachment.class,
            AttachmentSummary.class
        };
    }

    @Test
    public void test() throws URISyntaxException {
        final Path bookFilePath = Paths.get(Thread.currentThread().getContextClassLoader().getResource("ehcache.xml").toURI());
        final Path videoFilePath = Paths.get(Thread.currentThread().getContextClassLoader().getResource("spy.properties").toURI());

        AtomicReference<Long> bookIdHolder = new AtomicReference<>();
        AtomicReference<Long> videoIdHolder = new AtomicReference<>();

        doInJPA(entityManager -> {
            try {
                Attachment book = new Attachment();
                book.setName("High-Performance Java Persistence");
                book.setMediaType(MediaType.PDF);
                book.setContent(Files.readAllBytes(bookFilePath));
                entityManager.persist(book);

                Attachment video = new Attachment();
                video.setName("High-Performance Hibernate");
                video.setMediaType(MediaType.MPEG_VIDEO);
                video.setContent(Files.readAllBytes(videoFilePath));
                entityManager.persist(video);

                bookIdHolder.set(book.getId());
                videoIdHolder.set(video.getId());
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });

        doInJPA(entityManager -> {
            try {
                Long bookId = bookIdHolder.get();

                AttachmentSummary bookSummary = entityManager.find(AttachmentSummary.class, bookId);

                LOGGER.debug("Fetched book: {}", bookSummary.getName());

                Attachment book = entityManager.find(Attachment.class, bookId);
                assertArrayEquals(Files.readAllBytes(bookFilePath), book.getContent());
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });
    }

    @MappedSuperclass
    public static class BaseAttachment {

        @Id
        @GeneratedValue
        private Long id;

        private String name;

        @Enumerated
        @Column(name = "media_type")
        private MediaType mediaType;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public MediaType getMediaType() {
            return mediaType;
        }

        public void setMediaType(MediaType mediaType) {
            this.mediaType = mediaType;
        }
    }

    @Entity(name = "AttachmentSummary")
    @Table(name = "attachment")
    public static class AttachmentSummary extends BaseAttachment {}

    @Entity(name = "Attachment")
    @Table(name = "attachment")
    public static class Attachment extends BaseAttachment {

        @Lob
        private byte[] content;

        public byte[] getContent() {
            return content;
        }

        public void setContent(byte[] content) {
            this.content = content;
        }
    }
}
