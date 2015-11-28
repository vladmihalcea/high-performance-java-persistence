package com.vladmihalcea.book.hpjp.hibernate.type;

import java.io.Serializable;
import java.util.Objects;

/**
 * <code>IPv4</code> - IPv4 Address
 *
 * @author Vlad Mihalcea
 */
public class IPv4 implements Serializable {

    private final String address;

    public IPv4(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IPv4 IPv4 = (IPv4) o;
        return Objects.equals(address, IPv4.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}
