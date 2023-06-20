package com.vladmihalcea.hpjp.hibernate.type.money;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import io.hypersistence.utils.hibernate.type.money.MonetaryAmountType;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.CompositeType;
import org.javamoney.moneta.Money;
import org.junit.Test;

import javax.money.MonetaryAmount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MonetaryAmountTypeTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Product.class,
            ProductPricing.class
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Product()
                    .setId(1L)
                    .setName("Hypersistence Optimizer")
                    .addPricingPlan(
                        new ProductPricing()
                            .setName("Individual License")
                            .setType(PricingType.SUBSCRIPTION)
                            .setPrice(
                                Money.of(
                                    new BigDecimal("49.0"),
                                    "USD"
                                )
                            )
                    )
                    .addPricingPlan(
                        new ProductPricing()
                            .setName("5-Year Individual License")
                            .setType(PricingType.ONE_TIME_PURCHASE)
                            .setPrice(
                                Money.of(
                                    new BigDecimal("199.0"),
                                    "USD"
                                )
                            )
                    )
                    .addPricingPlan(
                        new ProductPricing()
                            .setName("10-Dev Group License")
                            .setType(PricingType.SUBSCRIPTION)
                            .setPrice(
                                Money.of(
                                    new BigDecimal("349.0"),
                                    "USD"
                                )
                            )
                    )
            );
        });

        doInJPA(entityManager -> {
            ProductPricing pricing = entityManager.createQuery("""
                select pp
                from ProductPricing pp
                where
                    pp.product.id = :productId and
                    pp.name = :name
                """, ProductPricing.class)
            .setParameter("productId", 1L)
            .setParameter("name", "Individual License")
            .getSingleResult();

            assertEquals(pricing.getPrice().getNumber().longValue(), 49);
            assertEquals(pricing.getPrice().getCurrency().getCurrencyCode(), "USD");
        });
    }

    @Entity(name = "Product")
    @Table(name = "product")
    public static class Product {

        @Id
        private Long id;

        private String name;

        @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
        )
        private List<ProductPricing> pricingPlans = new ArrayList<>();

        public Long getId() {
            return id;
        }

        public Product setId(Long id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public Product setName(String name) {
            this.name = name;
            return this;
        }

        public List<ProductPricing> getPricingPlans() {
            return pricingPlans;
        }

        public Product addPricingPlan(ProductPricing pricingPlan) {
            pricingPlans.add(pricingPlan);
            pricingPlan.setProduct(this);
            return this;
        }
    }

    @Entity(name = "ProductPricing")
    @Table(name = "product_pricing")
    public static class ProductPricing {

        @Id
        @GeneratedValue
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Product product;

        private String name;

        @Enumerated
        private PricingType type;

        @Columns(columns = {
            @Column(name = "price_amount"),
            @Column(name = "price_currency")
        })
        @CompositeType(MonetaryAmountType.class)
        private MonetaryAmount price;

        public Long getId() {
            return id;
        }

        public ProductPricing setId(Long id) {
            this.id = id;
            return this;
        }

        public Product getProduct() {
            return product;
        }

        public ProductPricing setProduct(Product product) {
            this.product = product;
            return this;
        }

        public String getName() {
            return name;
        }

        public ProductPricing setName(String name) {
            this.name = name;
            return this;
        }

        public PricingType getType() {
            return type;
        }

        public ProductPricing setType(PricingType type) {
            this.type = type;
            return this;
        }

        public MonetaryAmount getPrice() {
            return price;
        }

        public ProductPricing setPrice(MonetaryAmount salary) {
            this.price = salary;
            return this;
        }
    }

    public enum PricingType {
        ONE_TIME_PURCHASE,
        SUBSCRIPTION
    }
}
