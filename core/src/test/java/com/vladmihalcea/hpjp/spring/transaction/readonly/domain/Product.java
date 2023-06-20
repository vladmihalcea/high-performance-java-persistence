package com.vladmihalcea.hpjp.spring.transaction.readonly.domain;

import com.vladmihalcea.hpjp.spring.transaction.readonly.service.fxrate.FxCurrency;
import com.vladmihalcea.hpjp.spring.transaction.readonly.service.fxrate.FxRate;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "product")
public class Product {

    private static final MathContext CENTS = new MathContext(4, RoundingMode.HALF_UP);

    @Id
    private Long id;

    private String name;

    private BigDecimal price;

    @Enumerated
    private FxCurrency currency;

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

    public Product setName(String title) {
        this.name = title;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Product setPrice(BigDecimal priceInCents) {
        this.price = priceInCents;
        return this;
    }

    public FxCurrency getCurrency() {
        return currency;
    }

    public Product setCurrency(FxCurrency currency) {
        this.currency = currency;
        return this;
    }

    public void convertTo(FxCurrency currency, FxRate fxRate) {
        setPrice(price.multiply(fxRate.convert(this.currency, currency), CENTS));
        setCurrency(currency);
    }
}
