package com.vladmihalcea.hpjp.spring.transaction.readonly.service.fxrate;

import com.vladmihalcea.hpjp.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class FxRate {

    private LocalDate date;

    private BigDecimal USD;

    private BigDecimal EUR;

    private BigDecimal GBP;

    private BigDecimal CHF;

    private BigDecimal DKK;

    private BigDecimal SEK;

    private BigDecimal NOK;

    private BigDecimal HUF;

    private BigDecimal CZK;

    private BigDecimal PLN;

    private BigDecimal BGN;

    private BigDecimal HRK;

    private BigDecimal RON = BigDecimal.ONE;

    public LocalDate getDate() {
        return date;
    }

    public FxRate setDate(LocalDate date) {
        this.date = date;
        return this;
    }

    public BigDecimal getUSD() {
        return USD;
    }

    public FxRate setUSD(BigDecimal USD) {
        this.USD = USD;
        return this;
    }

    public BigDecimal getEUR() {
        return EUR;
    }

    public FxRate setEUR(BigDecimal EUR) {
        this.EUR = EUR;
        return this;
    }

    public BigDecimal getGBP() {
        return GBP;
    }

    public FxRate setGBP(BigDecimal GBP) {
        this.GBP = GBP;
        return this;
    }

    public BigDecimal getCHF() {
        return CHF;
    }

    public FxRate setCHF(BigDecimal CHF) {
        this.CHF = CHF;
        return this;
    }

    public BigDecimal getDKK() {
        return DKK;
    }

    public FxRate setDKK(BigDecimal DKK) {
        this.DKK = DKK;
        return this;
    }

    public BigDecimal getSEK() {
        return SEK;
    }

    public FxRate setSEK(BigDecimal SEK) {
        this.SEK = SEK;
        return this;
    }

    public BigDecimal getNOK() {
        return NOK;
    }

    public FxRate setNOK(BigDecimal NOK) {
        this.NOK = NOK;
        return this;
    }

    public BigDecimal getHUF() {
        return HUF;
    }

    public FxRate setHUF(BigDecimal HUF) {
        this.HUF = HUF;
        return this;
    }

    public BigDecimal getCZK() {
        return CZK;
    }

    public FxRate setCZK(BigDecimal CZK) {
        this.CZK = CZK;
        return this;
    }

    public BigDecimal getPLN() {
        return PLN;
    }

    public FxRate setPLN(BigDecimal PLN) {
        this.PLN = PLN;
        return this;
    }

    public BigDecimal getBGN() {
        return BGN;
    }

    public FxRate setBGN(BigDecimal BGN) {
        this.BGN = BGN;
        return this;
    }

    public BigDecimal getHRK() {
        return HRK;
    }

    public FxRate setHRK(BigDecimal HRK) {
        this.HRK = HRK;
        return this;
    }

    public BigDecimal getRON() {
        return RON;
    }

    public FxRate setRON(BigDecimal RON) {
        this.RON = RON;
        return this;
    }

    public BigDecimal convert(FxCurrency from, FxCurrency to) {
        BigDecimal fromRate = ReflectionUtils.getFieldValue(this, from.name());
        BigDecimal toRate = ReflectionUtils.getFieldValue(this, to.name());
        return fromRate.divide(toRate, 4, RoundingMode.HALF_UP);
    }

    public void setRate(String currency, BigDecimal fxRateValue) {
        Method setter = ReflectionUtils.getSetterOrNull(this, currency, fxRateValue.getClass());
        if (setter != null) {
            ReflectionUtils.invokeMethod(this, setter, fxRateValue);
        }
    }
}

