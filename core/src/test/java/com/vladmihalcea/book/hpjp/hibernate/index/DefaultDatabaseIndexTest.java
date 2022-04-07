package com.vladmihalcea.book.hpjp.hibernate.index;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import jakarta.persistence.*;
import java.util.*;

/**
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public class DefaultDatabaseIndexTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class, 
            PostComment.class
        };
    }

    private final Database database;

    public DefaultDatabaseIndexTest(Database database) {
        this.database = database;
    }

    @Parameterized.Parameters
    public static Collection<Database[]> rdbmsDataSourceProvider() {
        List<Database[]> databases = new ArrayList<>();
        databases.add(new Database[] {Database.ORACLE});
        databases.add(new Database[] {Database.SQLSERVER});
        databases.add(new Database[] {Database.POSTGRESQL});
        databases.add(new Database[] {Database.MYSQL});
        return databases;
    }

    @Override
    protected Database database() {
        return database;
    }

    private final String[] tables = new String[] {
        "post",
        "post_comment"
    };

    @Test
    public void test() {
        doInJPA(this::findIndexes);
    }

    private void findIndexes(EntityManager entityManager) {
        switch(database) {
            case ORACLE -> Arrays.stream(tables).forEach(table -> findOracleTableIndexes(entityManager, table));
            case SQLSERVER -> Arrays.stream(tables).forEach(table -> findSQLServerTableIndexes(entityManager, table));
            case POSTGRESQL -> Arrays.stream(tables).forEach(table -> findPostgreSQLTableIndexes(entityManager, table));
            case MYSQL -> Arrays.stream(tables).forEach(table -> findMySQLTableIndexes(entityManager, table));
        }
    }

    private void findOracleTableIndexes(EntityManager entityManager, String tableName) {
        findTableIndexes(entityManager, tableName, """
            SELECT
                ind.index_name AS index_name,
                CASE
                   WHEN ind.uniqueness = 'UNIQUE' THEN 1
                   WHEN ind.uniqueness = 'NONUNIQUE' THEN 0
                END AS is_unique,
                ind_col.column_name AS column_name
            FROM
                sys.all_indexes ind
            INNER JOIN
                sys.all_ind_columns ind_col ON
                    ind.owner = ind_col.index_owner AND
                    ind.index_name = ind_col.index_name
            WHERE
                lower(ind.table_name) = :tableName
            """
        );
    }

    private void findSQLServerTableIndexes(EntityManager entityManager, String tableName) {
        findTableIndexes(entityManager, tableName, """
            SELECT
                ind.name AS index_name,
                ind.is_unique AS is_unique,
                col.name AS column_name
            FROM
                sys.indexes ind
            INNER JOIN
                sys.index_columns ic ON  ind.object_id = ic.object_id AND ind.index_id = ic.index_id
            INNER JOIN
                sys.columns col ON ic.object_id = col.object_id AND ic.column_id = col.column_id
            INNER JOIN
                sys.tables t ON ind.object_id = t.object_id
            WHERE
                t.name = :tableName
            """
        );
    }

    private void findPostgreSQLTableIndexes(EntityManager entityManager, String tableName) {
        findTableIndexes(entityManager, addSchema(tableName), """
            SELECT
                i.relname AS index_name,
                ix.indisunique AS is_unique,
                a.attname AS column_name
            FROM
                pg_class c
            INNER JOIN
                pg_index ix ON c.oid = ix.indrelid
            INNER JOIN
                pg_class i ON ix.indexrelid = i.oid
            INNER JOIN
                pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(ix.indkey)
            WHERE
                c.oid = CAST(CAST(:tableName AS regclass) AS oid)
            ORDER BY
                array_position(ix.indkey, a.attnum)
            """
        );
    }

    private void findMySQLTableIndexes(EntityManager entityManager, String tableName) {
        findTableIndexes(entityManager, tableName, """
            SELECT
                INDEX_NAME AS index_name,
                !NON_UNIQUE AS is_unique,
                COLUMN_NAME as column_name
            FROM
                INFORMATION_SCHEMA.STATISTICS
            WHERE
                TABLE_NAME = :tableName
            """
        );
    }

    private void findTableIndexes(EntityManager entityManager, String tableName, String query) {
        List<Tuple> indexes = entityManager.createNativeQuery(query, Tuple.class)
            .setParameter("tableName", tableName)
            .getResultList();

        for (Tuple index : indexes) {
            LOGGER.info(
                "Database [{}], Table [{}], Column [{}], Index [{}], Unique [{}]",
                database,
                tableName,
                index.get("column_name"),
                index.get("index_name"),
                index.get("is_unique")
            );
        }
    }

    private String addSchema(String tableName) {
        return String.format("public.%s", tableName);
    }

    @Entity(name = "Post")
    @Table(
        name = "post",
        uniqueConstraints = @UniqueConstraint(
            name = "UK_POST_SLUG",
            columnNames = "slug"
        )
    )
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @Column
        private String slug;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Post post = (Post) o;
            return Objects.equals(slug, post.getSlug());
        }

        @Override
        public int hashCode() {
            return Objects.hash(slug);
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        private String review;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(
            name = "post_id",
            foreignKey = @ForeignKey(
                name = "FK_POST_COMMENT_POST_ID"
            )
        )
        private Post post;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PostComment)) return false;
            return id != null && id.equals(((PostComment) o).getId());
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }
}
