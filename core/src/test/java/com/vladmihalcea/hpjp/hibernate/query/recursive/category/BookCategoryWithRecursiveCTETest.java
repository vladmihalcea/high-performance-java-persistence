package com.vladmihalcea.hpjp.hibernate.query.recursive.category;

import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import com.vladmihalcea.hpjp.hibernate.query.dto.projection.transformer.DistinctListTransformer;
import com.vladmihalcea.hpjp.hibernate.query.recursive.category.model.Book;
import com.vladmihalcea.hpjp.hibernate.query.recursive.category.model.Category;
import com.vladmihalcea.hpjp.hibernate.query.recursive.category.model.CategoryView;
import com.vladmihalcea.hpjp.hibernate.query.recursive.category.model.Category_;
import com.vladmihalcea.hpjp.hibernate.query.recursive.category.model.dto.BookDTO;
import com.vladmihalcea.hpjp.hibernate.query.recursive.category.model.dto.CategoryDTO;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.TupleTransformer;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BookCategoryWithRecursiveCTETest extends AbstractTest {

    private CriteriaBuilderFactory criteriaBuilderFactory;

    @Override
    protected EntityManagerFactory newEntityManagerFactory() {
        EntityManagerFactory entityManagerFactory = super.newEntityManagerFactory();
        CriteriaBuilderConfiguration config = Criteria.getDefault();
        criteriaBuilderFactory = config.createCriteriaBuilderFactory(entityManagerFactory);
        return entityManagerFactory;
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Book.class,
            Category.class,
            CategoryView.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            Category java = new Category().setName("Java");
            Category jpa = new Category().setName("JPA").setParent(java);
            Category hibernate = new Category().setName("Hibernate").setParent(jpa);
            Category hibernate6 = new Category().setName("Hibernate 6").setParent(hibernate);

            entityManager.persist(java);
            entityManager.persist(jpa);
            entityManager.persist(hibernate);
            entityManager.persist(hibernate6);

            entityManager.flush();

            entityManager.persist(
                new Book()
                    .setTitle("High-Performance Java Persistence")
                    .setIsbn(9789730228236L)
                    .setCategory(hibernate6)
            );

            entityManager.persist(
                new Book()
                    .setTitle("Effective Java")
                    .setIsbn(9780134685991L)
                    .setCategory(java)
            );
        });
    }

    @Test
    public void testFetchManually() {
        Book hpjp = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select b
                from Book b
                left join fetch b.category c1
                left join fetch c1.parent c2
                left join fetch c2.parent c3
                left join fetch c3.parent c4
                where b.isbn = :isbn
                """, Book.class)
            .setParameter("isbn", 9789730228236L)
            .getSingleResult();
        });

        Category hpjpCategory = hpjp.getCategory();

        assertEquals("Hibernate 6", hpjpCategory.getName());
        assertEquals("Hibernate", hpjpCategory.getParent().getName());
        assertEquals("JPA", hpjpCategory.getParent().getParent().getName());
        assertEquals("Java", hpjpCategory.getParent().getParent().getParent().getName());

        Book effectiveJava = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select b
                from Book b
                left join fetch b.category c
                where b.isbn = :isbn
                """, Book.class)
            .setParameter("isbn", 9780134685991L)
            .getSingleResult();
        });

        assertEquals("Java", effectiveJava.getCategory().getName());
    }

    @Test
    public void testFetchAutomatically() {
        BookDTO hpjp = doInJPA(entityManager ->
            (BookDTO) entityManager.createNativeQuery("""
                SELECT 
                    b.id AS "b.id", 
                    b.title AS "b.title", 
                    b.isbn AS "b.isbn", 
                    b.category_id AS "b.category_id",
                    c.id AS "c.id", 
                    c.name AS "c.name", 
                    c.parent_id AS "c.parent_id"
                FROM
                    book b,
                LATERAL (
                    WITH RECURSIVE book_category_hierarchy AS (
                        SELECT
                            category.id AS id,
                            category.name AS name,
                            category.parent_id AS parent_id
                        FROM category
                        WHERE category.id = b.category_id
                        UNION ALL
                        SELECT
                            category.id AS id,
                            category.name AS name,
                            category.parent_id AS parent_id
                        FROM category category
                        JOIN book_category_hierarchy bch ON bch.parent_id = category.id
                    )
                    SELECT *
                    FROM book_category_hierarchy
                ) c
                WHERE isbn = :isbn                                      
                """, "BookCategory")
        .setParameter("isbn", 9789730228236L)
        .unwrap(NativeQuery.class)
        .setTupleTransformer(new BookDTOTupleTransformer())
        .setResultListTransformer(DistinctListTransformer.INSTANCE)
        .getSingleResult());

        CategoryDTO hpjpCategory = hpjp.getCategory();

        assertEquals("Hibernate 6", hpjpCategory.getName());
        assertEquals("Hibernate", hpjpCategory.getParent().getName());
        assertEquals("JPA", hpjpCategory.getParent().getParent().getName());
        assertEquals("Java", hpjpCategory.getParent().getParent().getParent().getName());
    }

    @Test
    public void testFetchAllParents() {
        List<CategoryDTO> categoryWithParents = doInJPA(entityManager -> {
            return entityManager.createNativeQuery("""
                SELECT
                    c.id AS "c.id", 
                    c.name AS "c.name", 
                    c.parent_id AS "c.parent_id"
                FROM (
                    WITH RECURSIVE book_category_hierarchy AS (
                        SELECT
                            category.id AS id,
                            category.name AS name,
                            category.parent_id AS parent_id
                        FROM category
                        WHERE category.name = :categoryName
                        UNION ALL
                        SELECT
                            category.id AS id,
                            category.name AS name,
                            category.parent_id AS parent_id
                        FROM category category
                        JOIN book_category_hierarchy bch ON bch.parent_id = category.id
                    )
                    SELECT *
                    FROM book_category_hierarchy
                ) c
                """, CategoryDTO.class)
                .setParameter("categoryName", "Hibernate 6")
                .getResultList();
            }
        );

        int index = 0;
        assertEquals(4, categoryWithParents.size());
        assertEquals("Hibernate 6", categoryWithParents.get(index++).getName());
        assertEquals("Hibernate", categoryWithParents.get(index++).getName());
        assertEquals("JPA", categoryWithParents.get(index++).getName());
        assertEquals("Java", categoryWithParents.get(index).getName());
    }

    public static class BookDTOTupleTransformer implements TupleTransformer<BookDTO> {

        private BookDTO book;

        @Override
        public BookDTO transformTuple(Object[] tuple, String[] aliases) {
            CategoryDTO category = (CategoryDTO) tuple[1];

            if(book == null) {
                book = (BookDTO) tuple[0];
                book.setCategory(category);
            } else {
                CategoryDTO childCategory = book.getCategory().findByParentId(category.getId());
                if (childCategory != null) {
                    childCategory.setParent(category);
                }
            }

            return book;
        }
    }

    @Test
    public void testFetchCategoriesWithBlaze() {
        List<CategoryView> categories = doInJPA(entityManager -> {
            return criteriaBuilderFactory
                .create(entityManager, CategoryView.class)
                .withRecursive(CategoryView.class)
                .from(Category.class, "c")
                .bind(Category_.ID).select("c.id")
                .bind(Category_.NAME).select("c.name")
                .bind(Category_.PARENT).select("c.parent")
                .where("c.id").in()
                .from(Book.class, "book")
                .select("book.category.id")
                .where("book.isbn").eqExpression(":isbn")
                .end()
                .unionAll()
                .from(Category.class, "c")
                .from(CategoryView.class, "child")
                .bind(Category_.ID).select("c.id")
                .bind(Category_.NAME).select("c.name")
                .bind(Category_.PARENT).select("c.parent")
                .where("c.id").eqExpression("child.parent.id")
                .end()
                .setParameter("isbn", 9789730228236L)
                .getResultList();
        });

        assertEquals(4, categories.size());
        CategoryView hibernateCategoryView = categories.get(0);
        assertEquals("Hibernate 6", hibernateCategoryView.getName());
        assertEquals("Hibernate", hibernateCategoryView.getParent().getName());
        assertEquals("JPA", hibernateCategoryView.getParent().getParent().getName());
        assertEquals("Java", hibernateCategoryView.getParent().getParent().getParent().getName());
    }
}
