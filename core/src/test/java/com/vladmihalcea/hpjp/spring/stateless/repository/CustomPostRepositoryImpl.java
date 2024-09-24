package com.vladmihalcea.hpjp.spring.stateless.repository;

import com.vladmihalcea.hpjp.spring.stateless.domain.BatchInsertPost;
import com.vladmihalcea.hpjp.spring.stateless.domain.Post;
import jakarta.persistence.EntityManager;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Vlad Mihalcea
 */
public class CustomPostRepositoryImpl implements CustomPostRepository<Post> {

    private final EntityManager entityManager;

    private final StatelessSessionBuilder statelessSessionBuilder;

    private final Integer batchProcessingSize;

    public CustomPostRepositoryImpl(
            EntityManager entityManager,
            StatelessSessionBuilder statelessSessionBuilder,
            Integer batchProcessingSize) {
        this.entityManager = entityManager;
        this.statelessSessionBuilder = statelessSessionBuilder;
        this.batchProcessingSize = batchProcessingSize;
    }

    @Override
    public <S extends Post> List<S> persistAll(Iterable<S> entities) {
        final StatelessSession statelessSession = statelessSessionBuilder
            .connection(
                entityManager
                    .unwrap(Session.class)
                    .doReturningWork(connection -> connection)
            )
            .openStatelessSession();
        try {
            statelessSession.setJdbcBatchSize(batchProcessingSize);
            statelessSession.beginTransaction();

            return StreamSupport.stream(entities.spliterator(), false)
                .peek(entity -> {
                    statelessSession.insert(BatchInsertPost.valueOf(entity));
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new HibernateException(e);
        } finally {
            JdbcSessionOwner jdbcSessionOwner = ((JdbcSessionOwner) statelessSession);
            jdbcSessionOwner.flushBeforeTransactionCompletion();
            statelessSession.close();
        }
    }
}
