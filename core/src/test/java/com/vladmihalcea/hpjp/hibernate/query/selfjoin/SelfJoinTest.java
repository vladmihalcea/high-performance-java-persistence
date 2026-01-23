package com.vladmihalcea.hpjp.hibernate.query.selfjoin;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.providers.SQLServerDataSourceProvider;
import jakarta.persistence.*;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author Vlad Mihalcea
 */
public class SelfJoinTest extends AbstractTest {
    
    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
        };
    }

    @Override
    protected Database database() {
        return Database.SQLSERVER;
    }

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Slf4jReporter logReporter = Slf4jReporter
        .forRegistry(metricRegistry)
        .outputTo(LOGGER)
        .build();

    private int BATCH_SIZE = 500;
    private int EXECUTION_COUNT = 100;

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
            AvailableSettings.STATEMENT_BATCH_SIZE, String.valueOf(BATCH_SIZE)
        );
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return ((SQLServerDataSourceProvider) super.dataSourceProvider())
            .setUseBulkCopyForBatchInsert(true)
            .setSendStringParametersAsUnicode(true);
    }

    private LocalDateTime MIDNIGHT = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);

    @Override
    public void afterInit() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        doInJPA(entityManager -> {
            int POST_COUNT = 100_000;

            //Old records
            for (long i = POST_COUNT; i >= 1; i--) {
                if(i % BATCH_SIZE == 0) {
                    entityManager.getTransaction().commit();
                    entityManager.getTransaction().begin();
                    entityManager.clear();
                }
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setCategory(i % 100)
                        .setPublished(true)
                        .setPublishedOn(MIDNIGHT.minusMinutes(10 * i))
                        .setUpdatedOn(MIDNIGHT.minusMinutes(i))
                        .setType(PostType.values()[(int) (i % PostType.values().length)])
                );
            }

            for (long i = 1; i <= 1000; i++) {
                boolean published = random.nextBoolean();
                entityManager.persist(
                    new Post()
                        .setId(POST_COUNT + i)
                        .setCategory(published ? i % 10 : i % 5)
                        .setPublished(published)
                        .setPublishedOn(published ? MIDNIGHT.plusMinutes(i) : null)
                        .setUpdatedOn(published ? MIDNIGHT.plusMinutes(i + 5) : null)
                        .setType(PostType.values()[random.nextInt(4)])
                );
            }
        });
    }

/*
select distinct
	p1.CategoryId as CategoryId
from Posts p1
where p1.PostType = 'QUESTION'
    and p1.PublishedOn is not null
    and p1.Published = 1
    and p1.UpdatedOn >= '2026-01-23 00:00:00.0'
    and p1.UpdatedOn <= '2026-01-24 00:00:00.0'
    and p1.CategoryId NOT IN (
        select p2.CategoryId
        from Posts p2
        where p2.PostType = 'QUESTION'
            and (p2.Published = 0 or
               p2.PublishedOn is null
            )
    )
 */
/*
type=TIMER, name=testNotInSingleIndex, count=100, min=14.613999999999999, max=469.0052, mean=28.655537039397146, stddev=43.90528903092145,
median=23.101499999999998, p75=27.469099999999997, p95=40.251599999999996, p98=43.0586, p99=45.9568, p999=469.0052, duration_unit=milliseconds
 */
    @Test
    public void testNotInSingleIndex() {
        executeStatement("""
            CREATE NONCLUSTERED INDEX POST_UPDATED_ON
            ON Posts (CategoryId, UpdatedOn, PublishedOn)
            INCLUDE (Published, PostType)
            """);
        executeStatement("UPDATE STATISTICS Posts WITH FULLSCAN");

        LOGGER.info("Test Not In with Single Index");
        Timer timer = metricRegistry.timer("testNotInSingleIndex");
        for (int i = 0; i < EXECUTION_COUNT; i++) {
            doInJPA(entityManager -> {
                long startNanos = System.nanoTime();
                List<Long> categoryIds = entityManager.createNativeQuery("""
                    select distinct
                        p1.CategoryId as CategoryId
                    from Posts p1
                    where p1.PostType = :postType
                        and p1.PublishedOn is not null
                        and p1.Published = :published
                        and p1.UpdatedOn >= :fromTimestamp
                        and p1.UpdatedOn <= :toTimestamp
                        and p1.CategoryId NOT IN (
                          select p2.CategoryId
                          from Posts p2
                          where p2.PostType = :postType
                              and (p2.Published = :unpublished or
                                   p2.PublishedOn is null
                               )
                    )
                    """, Long.class)
                    .setParameter("postType", PostType.QUESTION.name())
                    .setParameter("published", true)
                    .setParameter("unpublished", false)
                    .setParameter("fromTimestamp", MIDNIGHT)
                    .setParameter("toTimestamp", MIDNIGHT.plusDays(1))
                    .getResultList();
                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);

                LOGGER.info("Category Ids: {}", categoryIds);
            });
            logReporter.report();
        }
    }

/*
select distinct
	p1.CategoryId as CategoryId
from Posts p1
where p1.PostType = 'QUESTION'
    and p1.PublishedOn is not null
    and p1.Published = 1
    and p1.UpdatedOn >= '2026-01-23 00:00:00.0'
    and p1.UpdatedOn <= '2026-01-24 00:00:00.0'
    and p1.CategoryId NOT IN (
        select p2.CategoryId
        from Posts p2
        where p2.PostType = 'QUESTION'
            and (p2.Published = 0 or
               p2.PublishedOn is null
            )
    )
 */
/*
type=TIMER, name=testNotInTwoIndexes, count=100, min=6.4577, max=434.82239999999996, mean=12.710094676715988, stddev=42.018053121425595,
median=8.4331, p75=9.205, p95=12.2433, p98=13.0895, p99=14.2106, p999=434.82239999999996, duration_unit=milliseconds
 */
    @Test
    public void testNotInTwoIndexes() {
        executeStatement("""
            CREATE NONCLUSTERED INDEX POST_UPDATED_ON
            ON Posts (UpdatedOn, PostType)
            INCLUDE (CategoryId, Published)
            WHERE PublishedOn is not null;
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX POST_UNPUBLISHED
            ON Posts (CategoryId, PostType, Published, PublishedOn)
            """);
        executeStatement("UPDATE STATISTICS Posts WITH FULLSCAN");

        LOGGER.info("Test Not In with Two Indexes");
        Timer timer = metricRegistry.timer("testNotInTwoIndexes");
        for (int i = 0; i < EXECUTION_COUNT; i++) {
            doInJPA(entityManager -> {
                long startNanos = System.nanoTime();
                List<Long> categoryIds = entityManager.createNativeQuery("""
                    select distinct
                        p1.CategoryId as CategoryId
                    from Posts p1
                    where p1.PostType = :postType
                        and p1.PublishedOn is not null
                        and p1.Published = :published
                        and p1.UpdatedOn >= :fromTimestamp
                        and p1.UpdatedOn <= :toTimestamp
                        and p1.CategoryId NOT IN (
                          select p2.CategoryId
                          from Posts p2
                          where p2.PostType = :postType
                              and (p2.Published = :unpublished or
                                   p2.PublishedOn is null
                               )
                    )
                    """, Long.class)
                    .setParameter("postType", PostType.QUESTION.name())
                    .setParameter("published", true)
                    .setParameter("unpublished", false)
                    .setParameter("fromTimestamp", MIDNIGHT)
                    .setParameter("toTimestamp", MIDNIGHT.plusDays(1))
                    .getResultList();
                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);

                LOGGER.info("Category Ids: {}", categoryIds);
            });
            logReporter.report();
        }
    }

    /*
select distinct
	p1.CategoryId as CategoryId
from Posts p1
where p1.PostType = 'QUESTION'
    and p1.PublishedOn is not null
    and p1.Published = 1
    and p1.UpdatedOn >= '2026-01-23 00:00:00.0'
    and p1.UpdatedOn <= '2026-01-24 00:00:00.0'
    and NOT EXISTS (
        select 1
        from Posts p2
        where p2.PostType = 'QUESTION'
            and p2.CategoryId = p1.CategoryId
            and (p2.Published = 0 or
               p2.PublishedOn is null
            )
    )
     */
    /*
    type=TIMER, name=testNotExistsSingleIndex, count=100, min=14.4401, max=390.54249999999996, mean=24.087353835273547, stddev=36.64446404351388,
    median=18.673199999999998, p75=23.115199999999998, p95=31.0949, p98=37.609899999999996, p99=37.7706, p999=390.54249999999996, duration_unit=milliseconds
     */
    @Test
    public void testNotExistsSingleIndex() {
        executeStatement("""
            CREATE NONCLUSTERED INDEX POST_UPDATED_ON
            ON Posts (CategoryId, UpdatedOn, PublishedOn)
            INCLUDE (Published, PostType)
            """);
        executeStatement("UPDATE STATISTICS Posts WITH FULLSCAN");

        LOGGER.info("Test Not Exists with Single Index");
        Timer timer = metricRegistry.timer("testNotExistsSingleIndex");
        for (int i = 0; i < EXECUTION_COUNT; i++) {
            doInJPA(entityManager -> {
                long startNanos = System.nanoTime();
                List<Long> categoryIds = entityManager.createNativeQuery("""
                    select distinct
                        p1.CategoryId as CategoryId
                    from Posts p1
                    where p1.PostType = :postType
                        and p1.PublishedOn is not null
                        and p1.Published = :published
                        and p1.UpdatedOn >= :fromTimestamp
                        and p1.UpdatedOn <= :toTimestamp
                        and NOT EXISTS (
                          select 1
                          from Posts p2
                          where p2.PostType = :postType
                              and p2.CategoryId = p1.CategoryId
                              and (p2.Published = :unpublished or
                                   p2.PublishedOn is null
                               )
                    )
                    """, Long.class)
                .setParameter("postType", PostType.QUESTION.name())
                .setParameter("published", true)
                .setParameter("unpublished", false)
                .setParameter("fromTimestamp", MIDNIGHT)
                .setParameter("toTimestamp", MIDNIGHT.plusDays(1))
                .getResultList();
                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);

                LOGGER.info("Category Ids: {}", categoryIds);
            });
            logReporter.report();
        }
    }

    /*
select distinct
	p1.CategoryId as CategoryId
from Posts p1
where p1.PostType = 'QUESTION'
    and p1.PublishedOn is not null
    and p1.Published = 1
    and p1.UpdatedOn >= '2026-01-23 00:00:00.0'
    and p1.UpdatedOn <= '2026-01-24 00:00:00.0'
    and NOT EXISTS (
        select 1
        from Posts p2
        where p2.PostType = 'QUESTION'
            and p2.CategoryId = p1.CategoryId
            and (p2.Published = 0 or
               p2.PublishedOn is null
            )
    )
     */
    /*
    type=TIMER, name=testNotExistsTwoIndexes, count=100, min=2.9981999999999998, max=467.9756, mean=8.868317943374233, stddev=45.93655512440653,
    median=3.9027, p75=4.8503, p95=6.7277, p98=8.283299999999999, p99=8.565299999999999, p999=467.9756, duration_unit=milliseconds
     */
    @Test
    public void testNotExistsTwoIndexes() {
        executeStatement("""
            CREATE NONCLUSTERED INDEX POST_UPDATED_ON
            ON Posts (UpdatedOn, PostType)
            INCLUDE (CategoryId, Published)
            WHERE PublishedOn is not null;
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX POST_UNPUBLISHED
            ON Posts (CategoryId, PostType, Published, PublishedOn)
            """);
        executeStatement("UPDATE STATISTICS Posts WITH FULLSCAN");

        LOGGER.info("Test Not Exists with Two Indexes");
        Timer timer = metricRegistry.timer("testNotExistsTwoIndexes");
        for (int i = 0; i < EXECUTION_COUNT; i++) {
            doInJPA(entityManager -> {
                long startNanos = System.nanoTime();
                List<Long> categoryIds = entityManager.createNativeQuery("""
                    select distinct
                        p1.CategoryId as CategoryId
                    from Posts p1
                    where p1.PostType = :postType
                        and p1.PublishedOn is not null
                        and p1.Published = :published
                        and p1.UpdatedOn >= :fromTimestamp
                        and p1.UpdatedOn <= :toTimestamp
                        and NOT EXISTS (
                          select 1
                          from Posts p2
                          where p2.PostType = :postType
                              and p2.CategoryId = p1.CategoryId
                              and (p2.Published = :unpublished or
                                   p2.PublishedOn is null
                               )
                    )
                    """, Long.class)
                .setParameter("postType", PostType.QUESTION.name())
                .setParameter("published", true)
                .setParameter("unpublished", false)
                .setParameter("fromTimestamp", MIDNIGHT)
                .setParameter("toTimestamp", MIDNIGHT.plusDays(1))
                .getResultList();
                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);

                LOGGER.info("Category Ids: {}", categoryIds);
            });
            logReporter.report();
        }
    }

/*
select distinct
    p1.CategoryId as CategoryId
from Posts p1
left join (
    select CategoryId
    from Posts p2
    where p2.PostType = 'QUESTION'
      and (p2.Published = 0 or
           p2.PublishedOn is null
       )
) p on p.CategoryId = p1.CategoryId
where p1.PostType = 'QUESTION'
    and p1.PublishedOn is not null
    and p1.Published = 1
    and p1.UpdatedOn >= '2026-01-23 00:00:00.0'
    and p1.UpdatedOn <= '2026-01-24 00:00:00.0'
    and p.CategoryId IS NULL
 */
/*
type=TIMER, name=testNotExistsTwoIndexes, count=100, min=27.9077, max=426.8503, mean=43.891210401044674, stddev=38.74556171890509,
median=38.842, p75=44.1611, p95=58.3841, p98=61.8615, p99=79.8669, p999=426.8503, duration_unit=milliseconds
 */
    @Test
    public void testLeftJoinSingleIndex() {
        executeStatement("""
            CREATE NONCLUSTERED INDEX POST_UPDATED_ON
            ON Posts (CategoryId, UpdatedOn, PublishedOn)
            INCLUDE (Published, PostType)
            """);
        executeStatement("UPDATE STATISTICS Posts WITH FULLSCAN");

        LOGGER.info("Test Left Join with One Index");
        Timer timer = metricRegistry.timer("testLeftJoinSingleIndex");
        for (int i = 0; i < EXECUTION_COUNT; i++) {
            doInJPA(entityManager -> {
                long startNanos = System.nanoTime();
                List<Long> categoryIds = entityManager.createNativeQuery("""
                    select distinct
                        p1.CategoryId as CategoryId
                    from Posts p1
                    left join (
                        select CategoryId
                        from Posts p2
                        where p2.PostType = :postType
                          and (p2.Published = :unpublished or
                               p2.PublishedOn is null
                           )
                    ) p on p.CategoryId = p1.CategoryId
                    where p1.PostType = :postType
                        and p1.PublishedOn is not null
                        and p1.Published = :published
                        and p1.UpdatedOn >= :fromTimestamp
                        and p1.UpdatedOn <= :toTimestamp
                        and p.CategoryId IS NULL
                    """, Long.class)
                .setParameter("postType", PostType.QUESTION.name())
                .setParameter("published", true)
                .setParameter("unpublished", false)
                .setParameter("fromTimestamp", MIDNIGHT)
                .setParameter("toTimestamp", MIDNIGHT.plusDays(1))
                .getResultList();
                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);

                LOGGER.info("Category Ids: {}", categoryIds);
            });
            logReporter.report();
        }
    }

/*
select distinct
    p1.CategoryId as CategoryId
from Posts p1
left join (
    select CategoryId
    from Posts p2
    where p2.PostType = 'QUESTION'
      and (p2.Published = 0 or
           p2.PublishedOn is null
       )
) p on p.CategoryId = p1.CategoryId
where p1.PostType = 'QUESTION'
    and p1.PublishedOn is not null
    and p1.Published = 1
    and p1.UpdatedOn >= '2026-01-23 00:00:00.0'
    and p1.UpdatedOn <= '2026-01-24 00:00:00.0'
    and p.CategoryId IS NULL
 */
/*
type=TIMER, name=testLeftJoinTwoIndexes, count=100, min=16.867, max=479.6581, mean=34.413532609994874, stddev=53.020334379578834,
median=25.229799999999997, p75=29.8169, p95=44.3074, p98=92.6203, p99=310.2837, p999=479.6581, duration_unit=milliseconds
 */
    @Test
    public void testLeftJoinTwoIndexes() {
        executeStatement("""
            CREATE NONCLUSTERED INDEX POST_UPDATED_ON
            ON Posts (UpdatedOn, PostType)
            INCLUDE (CategoryId, Published)
            WHERE PublishedOn is not null;
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX POST_UNPUBLISHED
            ON Posts (CategoryId, PostType, Published, PublishedOn)
            """);
        executeStatement("UPDATE STATISTICS Posts WITH FULLSCAN");

        LOGGER.info("Test Left Join with Two Indexes");
        Timer timer = metricRegistry.timer("testLeftJoinTwoIndexes");
        for (int i = 0; i < EXECUTION_COUNT; i++) {
            doInJPA(entityManager -> {
                long startNanos = System.nanoTime();
                List<Long> categoryIds = entityManager.createNativeQuery("""
                    select distinct
                        p1.CategoryId as CategoryId
                    from Posts p1
                    left join (
                        select CategoryId
                        from Posts p2
                        where p2.PostType = :postType
                          and (p2.Published = :unpublished or
                               p2.PublishedOn is null
                           )
                    ) p on p.CategoryId = p1.CategoryId
                    where p1.PostType = :postType
                        and p1.PublishedOn is not null
                        and p1.Published = :published
                        and p1.UpdatedOn >= :fromTimestamp
                        and p1.UpdatedOn <= :toTimestamp
                        and p.CategoryId IS NULL
                    """, Long.class)
                .setParameter("postType", PostType.QUESTION.name())
                .setParameter("published", true)
                .setParameter("unpublished", false)
                .setParameter("fromTimestamp", MIDNIGHT)
                .setParameter("toTimestamp", MIDNIGHT.plusDays(1))
                .getResultList();

                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);

                LOGGER.info("Category Ids: {}", categoryIds);
            });
            logReporter.report();
        }
    }

/*
select
    p1.CategoryId as CategoryId
from Posts p1
where p1.PostType = 'QUESTION'
    and p1.PublishedOn is not null
    and p1.Published = 1
    and p1.UpdatedOn >= '2026-01-23 00:00:00.0'
    and p1.UpdatedOn <= '2026-01-24 00:00:00.0'
except
select p2.CategoryId as CategoryId
from Posts p2
where p2.PostType = 'QUESTION'
    and (p2.Published = 0 or
       p2.PublishedOn is null
    )
 */
/*
type=TIMER, name=testExceptSingleIndex, count=100, min=14.4862, max=411.9232, mean=23.629076243336932, stddev=38.82356632135798,
median=18.3839, p75=21.9224, p95=32.3331, p98=35.4494, p99=36.1776, p999=411.9232, duration_unit=milliseconds
 */
    @Test
    public void testExceptSingleIndex() {
        executeStatement("""
            CREATE NONCLUSTERED INDEX POST_UPDATED_ON
            ON Posts (CategoryId, UpdatedOn, PublishedOn)
            INCLUDE (Published, PostType)
            """);
        executeStatement("UPDATE STATISTICS Posts WITH FULLSCAN");

        LOGGER.info("Test Except with Single Index");
        Timer timer = metricRegistry.timer("testExceptSingleIndex");
        for (int i = 0; i < EXECUTION_COUNT; i++) {
            doInJPA(entityManager -> {
                long startNanos = System.nanoTime();
                List<Long> categoryIds = entityManager.createNativeQuery("""
                select p1.CategoryId as CategoryId
                from Posts p1
                where p1.PostType = :postType
                    and p1.PublishedOn is not null
                    and p1.Published = :published
                    and p1.UpdatedOn >= :fromTimestamp
                    and p1.UpdatedOn <= :toTimestamp
                except
                select p2.CategoryId as CategoryId
                from Posts p2
                where p2.PostType = :postType
                    and (p2.Published = :unpublished or
                       p2.PublishedOn is null
                    )
                """, Long.class)
                    .setParameter("postType", PostType.QUESTION.name())
                    .setParameter("published", true)
                    .setParameter("unpublished", false)
                    .setParameter("fromTimestamp", MIDNIGHT)
                    .setParameter("toTimestamp", MIDNIGHT.plusDays(1))
                    .getResultList();

                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);

                LOGGER.info("Category Ids: {}", categoryIds);
            });
            logReporter.report();
        }
    }

/*
select
    p1.CategoryId as CategoryId
from Posts p1
where p1.PostType = 'QUESTION'
    and p1.PublishedOn is not null
    and p1.Published = 1
    and p1.UpdatedOn >= '2026-01-23 00:00:00.0'
    and p1.UpdatedOn <= '2026-01-24 00:00:00.0'
except
select p2.CategoryId as CategoryId
from Posts p2
where p2.PostType = 'QUESTION'
    and (p2.Published = 0 or
       p2.PublishedOn is null
    )
 */
/*
type=TIMER, name=testExceptTwoIndexes, count=100, min=2.8699, max=437.4333, mean=8.521222992018002, stddev=42.84235913607377,
median=3.9255, p75=4.7173, p95=6.4738999999999995, p98=7.2462, p99=7.707999999999999, p999=437.4333, duration_unit=milliseconds
 */
    @Test
    public void testExceptTwoIndexes() {
        executeStatement("""
            CREATE NONCLUSTERED INDEX POST_UPDATED_ON
            ON Posts (UpdatedOn, PostType)
            INCLUDE (CategoryId, Published)
            WHERE PublishedOn is not null;
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX POST_UNPUBLISHED
            ON Posts (CategoryId, PostType, Published, PublishedOn)
            """);
        executeStatement("UPDATE STATISTICS Posts WITH FULLSCAN");

        LOGGER.info("Test Except with Two Indexes");
        Timer timer = metricRegistry.timer("testExceptTwoIndexes");
        for (int i = 0; i < EXECUTION_COUNT; i++) {
            doInJPA(entityManager -> {
                long startNanos = System.nanoTime();
                List<Long> categoryIds = entityManager.createNativeQuery("""
                
                        select p1.CategoryId as CategoryId
                from Posts p1
                where p1.PostType = :postType
                    and p1.PublishedOn is not null
                    and p1.Published = :published
                    and p1.UpdatedOn >= :fromTimestamp
                    and p1.UpdatedOn <= :toTimestamp
                except
                select p2.CategoryId as CategoryId
                from Posts p2
                where p2.PostType = :postType
                    and (p2.Published = :unpublished or
                       p2.PublishedOn is null
                    )
                """, Long.class)
                .setParameter("postType", PostType.QUESTION.name())
                .setParameter("published", true)
                .setParameter("unpublished", false)
                .setParameter("fromTimestamp", MIDNIGHT)
                .setParameter("toTimestamp", MIDNIGHT.plusDays(1))
                .getResultList();

                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);

                LOGGER.info("Category Ids: {}", categoryIds);
            });
            logReporter.report();
        }
    }

    @Entity(name = "Post")
    @Table(name = "Posts")
    public static class Post {

        @Id
        @Column(name = "Id")
        private Long id;

        @Column(name = "PostType")
        @Enumerated(EnumType.STRING)
        private PostType type;

        @Column(name = "PublishedOn")
        private LocalDateTime publishedOn;

        @Column(name = "UpdatedOn")
        private LocalDateTime updatedOn;

        @Column(name = "Published")
        private boolean published;

        @Column(name = "CategoryId")
        private Long category;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public PostType getType() {
            return type;
        }

        public Post setType(PostType type) {
            this.type = type;
            return this;
        }

        public LocalDateTime getPublishedOn() {
            return publishedOn;
        }

        public Post setPublishedOn(LocalDateTime publishedOn) {
            this.publishedOn = publishedOn;
            return this;
        }

        public LocalDateTime getUpdatedOn() {
            return updatedOn;
        }

        public Post setUpdatedOn(LocalDateTime updatedOn) {
            this.updatedOn = updatedOn;
            return this;
        }

        public boolean isPublished() {
            return published;
        }

        public Post setPublished(boolean published) {
            this.published = published;
            return this;
        }

        public Long getCategory() {
            return category;
        }

        public Post setCategory(Long category) {
            this.category = category;
            return this;
        }
    }

    public enum PostType {
        QUESTION,
        ANSWER,
        WIKI,
        ARTICLE
    }

}
