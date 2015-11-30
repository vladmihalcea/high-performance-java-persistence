package com.vladmihalcea.book.hpjp.hibernate.type;

import java.io.Serializable;
import java.net.*;
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
        return Objects.equals(address, IPv4.class.cast(o).address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    public InetAddress toInetAddress() throws UnknownHostException {
        return Inet4Address.getByName(address);
    }
}
