package com.vladmihalcea.hpjp.spring.data.bytecode;

import com.vladmihalcea.hpjp.hibernate.forum.Attachment;
import com.vladmihalcea.hpjp.hibernate.forum.MediaType;
import com.vladmihalcea.hpjp.spring.data.bytecode.repository.AttachmentRepository;
import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import io.hypersistence.utils.common.ReflectionUtils;
import org.hibernate.LazyInitializationException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.aspectj.bridge.MessageUtil.fail;
import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class AttachmentLazyLoading {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final TransactionTemplate transactionTemplate;

    private final AttachmentRepository attachmentRepository;

    public AttachmentLazyLoading(
            TransactionTemplate transactionTemplate,
            AttachmentRepository attachmentRepository) {
        this.transactionTemplate = transactionTemplate;
        this.attachmentRepository = attachmentRepository;
    }

    public void test() throws URISyntaxException {
        final String bookFilePath = "ehcache.xml";
        final String videoFilePath = "spy.properties";

        transactionTemplate.execute(status -> {
            attachmentRepository.save(
                new Attachment()
                    .setId(1L)
                    .setName("High-Performance Java Persistence")
                    .setMediaType(MediaType.PDF)
                    .setContent(readBytes(bookFilePath))
            );

            attachmentRepository.save(
                new Attachment()
                    .setId(2L)
                    .setName("High-Performance Java Persistence - Mach 2")
                    .setMediaType(MediaType.MPEG_VIDEO)
                    .setContent(readBytes(videoFilePath))
            );

            return null;
        });

        transactionTemplate.execute(status -> {
            Attachment book = attachmentRepository.findById(1L).orElseThrow(null);
            LOGGER.debug("Fetched book: {}", book.getName());
            assertArrayEquals(readBytes(bookFilePath), book.getContent());

            Attachment video = attachmentRepository.findById(2L).orElseThrow(null);
            LOGGER.debug("Fetched video: {}", video.getName());
            assertArrayEquals(readBytes(videoFilePath), video.getContent());

            assertNotNull(ReflectionUtils.getFieldValue(book, "$$_hibernate_entityEntryHolder"));
            return null;
        });

        Attachment book = transactionTemplate.execute(
            status -> attachmentRepository.findById(1L).orElse(null)
        );

        try {
            book.getContent();

            fail("Should throw LazyInitializationException");
        } catch (Exception expected) {
            assertTrue(LazyInitializationException.class.isInstance(ExceptionUtil.rootCause(expected)));
        }
    }

    private byte[] readBytes(String path) {
        try {
            return Files.readAllBytes(
                Paths.get(Thread.currentThread().getContextClassLoader().getResource(path).toURI())
            );
        } catch (IOException|URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

