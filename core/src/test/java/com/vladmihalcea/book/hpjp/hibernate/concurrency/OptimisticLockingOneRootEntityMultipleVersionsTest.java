package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.ThrowableCauseMatcher;
import org.junit.rules.ExpectedException;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;


/**
 * EntityOptimisticLockingHighUpdateRateSingleEntityTest - Test to check optimistic checking on a single entity being updated by many threads
 *
 * @author Vlad Mihalcea
 */
public class OptimisticLockingOneRootEntityMultipleVersionsTest extends AbstractTest {

    private ExecutorService executorService = Executors.newFixedThreadPool(10);
    private Product originalProduct;

    @Before
    public void addProduct() {
        originalProduct = doInJPA(entityManager -> {
            Product product = Product.newInstance();
            product.setId(1L);
            product.setName("TV");
            product.setDescription("Plasma TV");
            product.setPrice(BigDecimal.valueOf(199.99));
            product.setQuantity(7L);
            entityManager.persist(product);
            return product;
        });
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    static interface DoWithProduct {
        void with(Product product);
    }

    public static class ModifyQuantity implements DoWithProduct {
        private final Long newQuantity;

        public ModifyQuantity(Long newQuantity) {
            this.newQuantity = newQuantity;
        }

        public void with(Product product) {
            product.setQuantity(newQuantity);
        }
    }

    public static class ModifyDescription implements DoWithProduct {
        private final String newDesc;

        public ModifyDescription(String newDesc) {
            this.newDesc = newDesc;
        }


        public void with(Product product) {
            product.setDescription(newDesc);
        }
    }

    public static class IncLikes implements DoWithProduct {

        public void with(Product product) {
            product.incrementLikes();
        }
    }

    public class TransactionTemplate implements VoidCallable {
        private final DoWithProduct doWithProduct;
        private CyclicBarrier barrier;

        public TransactionTemplate(DoWithProduct doWithProduct, CyclicBarrier barrier) {
            this.doWithProduct = doWithProduct;
            this.barrier = barrier;
        }

        public void execute() {
            doInJPA(entityManager -> {
                try {
                    Product product = (Product) entityManager.find(Product.class, 1L);
                    barrier.await();
                    doWithProduct.with(product);
                    return null;
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }


    public Product getProductById(final long productId) {
        return doInJPA(entityManager -> (Product) entityManager.find(Product.class, productId));
    }

    @Test
    public void canConcurrentlyModifyEachOfSubEntities() throws InterruptedException, ExecutionException {
        executeOperations(
                new IncLikes(),
                new ModifyDescription("Plasma HDTV"),
                new ModifyQuantity(1000L));

        Product modifiedProduct = getProductById(originalProduct.getId());

        assertThat(modifiedProduct.getDescription(), equalTo("Plasma HDTV"));
        assertThat(modifiedProduct.getQuantity(), equalTo(1000L));
        assertThat(modifiedProduct.getLikes(), equalTo(originalProduct.getLikes() + 1));
    }


    @Test
    public void optimisticLockingViolationForConcurrentStockModifications() throws InterruptedException, ExecutionException {
        expectedException.expectCause(new ThrowableCauseMatcher<>(IsInstanceOf.<Throwable>instanceOf(OptimisticLockException.class)));

        executeOperations(
                new IncLikes(),
                new ModifyQuantity(100L),
                new ModifyDescription("Plasma HDTV"),
                new ModifyQuantity(1000L));
    }

    @Test
    public void optimisticLockingViolationForConcurrentProductModifications() throws InterruptedException, ExecutionException {
        expectedException.expectCause(new ThrowableCauseMatcher<>(IsInstanceOf.<Throwable>instanceOf(OptimisticLockException.class)));

        executeOperations(
                new IncLikes(),
                new ModifyDescription("LCD TV"),
                new ModifyDescription("Plasma HDTV"),
                new ModifyQuantity(1L));
    }


    @Test
    public void optimisticLockingViolationForConcurrentLikeModifications() throws InterruptedException, ExecutionException {
        expectedException.expectCause(new ThrowableCauseMatcher<>(IsInstanceOf.<Throwable>instanceOf(OptimisticLockException.class)));

        executeOperations(
                new IncLikes(),
                new IncLikes(),
                new ModifyDescription("Plasma HDTV"),
                new ModifyQuantity(2L));

    }

    private void executeOperations(DoWithProduct... operations) throws InterruptedException, ExecutionException {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(operations.length);
        List<TransactionTemplate> tasks = new LinkedList<TransactionTemplate>();
        for (DoWithProduct operation : operations) {
            tasks.add(new TransactionTemplate(operation, cyclicBarrier));
        }

        List<Future<Void>> futures = executorService.invokeAll(tasks);

        for (Future<Void> future : futures) {
            future.get();
        }
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Product.class,
                ProductStock.class,
                ProductLiking.class
        };
    }

    /**
     * ProductStock - Product Stock
     *
     * @author Vlad Mihalcea
     */
    @Entity(name = "ProductStock")
    @Table(name = "product_stock")
    public static class ProductStock {

        @Id
        private Long id;

        @MapsId
        @OneToOne
        private Product product;

        private long quantity;

        @Version
        private int version;

        public Long getId() {
            return id;
        }

        public Product getProduct() {
            return product;
        }

        public void setProduct(Product product) {
            this.product = product;
        }

        public long getQuantity() {
            return quantity;
        }

        public void setQuantity(long quantity) {
            this.quantity = quantity;
        }
    }

    /**
     * ProductStock - Product Stock
     *
     * @author Vlad Mihalcea
     */
    @Entity(name = "ProductLiking")
    @Table(name = "product_liking")
    public static class ProductLiking {

        @Id
        private Long id;

        @MapsId
        @OneToOne
        private Product product;

        private int likes;

        @Version
        private int version;

        public Long getId() {
            return id;
        }

        public Product getProduct() {
            return product;
        }

        public void setProduct(Product product) {
            this.product = product;
        }

        public int getLikes() {
            return likes;
        }

        public int incrementLikes() {
            return ++likes;
        }
    }

    /**
     * Product - Product
     *
     * @author Vlad Mihalcea
     */
    @Entity(name = "Product")
    @Table(name = "product")
    public static class Product {

        public static Product newInstance() {
            Product product = new Product();
            ProductStock stock = new ProductStock();
            stock.setProduct(product);
            product.stock = stock;
            ProductLiking liking = new ProductLiking();
            liking.setProduct(product);
            product.liking = liking;
            return product;
        }

        @Id
        private Long id;

        @Column(unique = true, nullable = false)
        private String name;

        @Column(nullable = false)
        private String description;

        @Column(nullable = false)
        private BigDecimal price;

        @OneToOne(optional = false, mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
        @PrimaryKeyJoinColumn
        private ProductStock stock;

        @OneToOne(optional = false, mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
        @PrimaryKeyJoinColumn
        private ProductLiking liking;

        @Version
        private int version;

        public Product() {
        }

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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public long getQuantity() {
            return stock.getQuantity();
        }

        public void setQuantity(long quantity) {
            stock.setQuantity(quantity);
        }

        public int getLikes() {
            return liking.getLikes();
        }

        public int incrementLikes() {
            return liking.incrementLikes();
        }
    }
}
