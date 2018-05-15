package com.sorinc.test.domain;


import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class JwTBody {

    private String sub, jti, iss;
    private long exp, iat;

    public JwTBody() {
    }

    public String getSub() {
        return sub;
    }

    public String getIss() {
        return iss;
    }

    public long getExp() {
        return exp;
    }

    public long getIat() {
        return iat;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }

    public void setIat(long iat) {
        this.iat = iat;
    }

    public boolean isExpired() {
        Instant createAt = Instant.ofEpochMilli(iat);
        return createAt.plus(exp, ChronoUnit.MILLIS).isAfter(Instant.now());
    }
}
