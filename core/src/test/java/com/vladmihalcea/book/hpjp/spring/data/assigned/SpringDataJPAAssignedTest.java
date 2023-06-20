package com.vladmihalcea.book.hpjp.spring.data.assigned;

import com.vladmihalcea.book.hpjp.spring.data.assigned.config.SpringDataJPAAssignedConfiguration;
import com.vladmihalcea.book.hpjp.spring.data.assigned.domain.Book;
import com.vladmihalcea.book.hpjp.spring.data.assigned.repository.BookRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPAAssignedConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPAAssignedTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private BookRepository bookRepository;

    @Test
    public void testJpaRepositorySave() {
        transactionTemplate.execute(status -> {
            bookRepository.save(
                new Book()
                    .setIsbn(9789730228236L)
                    .setTitle("High-Performance Java Persistence")
                    .setAuthor("Vlad Mihalcea")
            );

            return null;
        });
    }

    @Test
    public void testBaseJpaRepositoryPersist() {
        transactionTemplate.execute(status -> {
            bookRepository.save(
                new Book()
                    .setIsbn(9789730228236L)
                    .setTitle("High-Performance Java Persistence")
                    .setAuthor("Vlad Mihalcea")
            );

            return null;
        });
    }
}

