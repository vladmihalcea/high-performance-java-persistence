package com.vladmihalcea.hpjp.hibernate.mapping.types;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class EmbeddedTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    protected void additionalProperties(Properties properties) {
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "none");
    }

    @Override
    protected void beforeInit() {
        executeStatement("drop table if exists post cascade");
        executeStatement("create table post (id integer not null, title varchar(100), status tinyint check (status between 0 and 2), created_on datetime(6), created_by varchar(100), primary key (id))");
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1)
                    .setTitle("High-Performance Java Persistence")
                    .setStatus(PostStatus.PENDING)
                    .setCreationDetails(
                        new CreationDetails()
                            .setCreatedBy("Vlad Mihalcea")
                    )
            );
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            assertEquals("Vlad Mihalcea", post.getCreationDetails().getCreatedBy());
        });
    }

}
