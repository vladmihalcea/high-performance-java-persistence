package com.vladmihalcea.hpjp.spring.data.assigned;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.assigned.config.SpringDataJPAAssignedConfiguration;
import com.vladmihalcea.hpjp.spring.data.assigned.domain.Book;
import com.vladmihalcea.hpjp.spring.data.assigned.repository.BookBaseJpaRepository;
import com.vladmihalcea.hpjp.spring.data.assigned.repository.BookRepository;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPAAssignedConfiguration.class)
public class SpringDataJPAAssignedTest extends AbstractSpringTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookBaseJpaRepository bookBaseJpaRepository;

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Book.class
        };
    }

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
            bookBaseJpaRepository.persist(
                new Book()
                    .setIsbn(9789730228236L)
                    .setTitle("High-Performance Java Persistence")
                    .setAuthor("Vlad Mihalcea")
            );

            return null;
        });
    }
}

