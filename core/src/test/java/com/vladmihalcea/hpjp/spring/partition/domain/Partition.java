package com.vladmihalcea.hpjp.spring.partition.domain;

/**
 * @author Vlad Mihalcea
 */
public enum Partition {
    ASIA("Asia"),
    AFRICA("Africa"),
    NORTH_AMERICA("North America"),
    SOUTH_AMERICA("South America"),
    EUROPE("Europe"),
    AUSTRALIA("Australia"),
    ;

    private final String key;

    Partition(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
