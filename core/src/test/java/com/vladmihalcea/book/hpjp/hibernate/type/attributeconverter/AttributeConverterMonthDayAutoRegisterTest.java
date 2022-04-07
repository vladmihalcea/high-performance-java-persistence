package com.vladmihalcea.book.hpjp.hibernate.type.attributeconverter;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.junit.Test;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class AttributeConverterMonthDayAutoRegisterTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            AnnualSubscription.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
            "hibernate.metadata_builder_contributor",
            AttributeConverterMetadataBuilderContributor.class.getName()
        );
    }


    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new AnnualSubscription()
                    .setId(1L)
                    .setPriceInCents(700)
                    .setPaymentDay(
                        MonthDay.of(Month.AUGUST, 17)
                    )
            );
        });

        doInJPA(entityManager -> {
            AnnualSubscription subscription = entityManager.find(AnnualSubscription.class, 1L);

            assertEquals(MonthDay.of(Month.AUGUST, 17), subscription.getPaymentDay());
        });
    }

    @Entity(name = "AnnualSubscription")
    @Table(name = "annual_subscription")
    public static class AnnualSubscription {

        @Id
        private Long id;

        @Column(name = "price_in_cents")
        private int priceInCents;

        @Column(name = "payment_day", columnDefinition = "date")
        private MonthDay paymentDay;

        public Long getId() {
            return id;
        }

        public AnnualSubscription setId(Long id) {
            this.id = id;
            return this;
        }

        public int getPriceInCents() {
            return priceInCents;
        }

        public AnnualSubscription setPriceInCents(int priceInCents) {
            this.priceInCents = priceInCents;
            return this;
        }

        public MonthDay getPaymentDay() {
            return paymentDay;
        }

        public AnnualSubscription setPaymentDay(MonthDay paymentDay) {
            this.paymentDay = paymentDay;
            return this;
        }
    }

    @Converter(autoApply = true)
    public static class MonthDayDateAttributeConverter
        implements AttributeConverter<MonthDay, java.sql.Date> {

        @Override
        public java.sql.Date convertToDatabaseColumn(MonthDay monthDay) {
            if (monthDay != null) {
                return java.sql.Date.valueOf(
                    monthDay.atYear(1)
                );
            }
            return null;
        }

        @Override
        public MonthDay convertToEntityAttribute(java.sql.Date date) {
            if (date != null) {
                LocalDate localDate = date.toLocalDate();
                return MonthDay.of(localDate.getMonth(), localDate.getDayOfMonth());
            }
            return null;
        }
    }

    public static class AttributeConverterMetadataBuilderContributor
            implements MetadataBuilderContributor {

        @Override
        public void contribute(MetadataBuilder metadataBuilder) {
            metadataBuilder.applyAttributeConverter(MonthDayDateAttributeConverter.class);
        }
    }
}
