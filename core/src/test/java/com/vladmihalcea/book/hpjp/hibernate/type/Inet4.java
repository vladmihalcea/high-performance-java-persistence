package com.vladmihalcea.book.hpjp.hibernate.type;

import java.io.Serializable;
import java.util.Objects;

/**
 * <code>Inet4</code> - Inet4 Address
 *
 * @author Vlad Mihalcea
 */
public class Inet4 implements Serializable {

    private final String address;

    public Inet4(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inet4 inet4 = (Inet4) o;
        return Objects.equals(address, inet4.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}
