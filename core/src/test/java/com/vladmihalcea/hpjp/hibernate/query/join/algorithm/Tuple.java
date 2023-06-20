package com.vladmihalcea.hpjp.hibernate.query.join.algorithm;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vlad Mihalcea
 */
public class Tuple {

    private final Map<String, Object> valueMap = new HashMap<>();

    public Tuple add(String alias, Object value) {
        valueMap.put(alias, value);
        return this;
    }

    public <E> E get(String alias) {
        return (E) valueMap.get(alias);
    }

    public long getLong(String alias) {
        return (Long) valueMap.get(alias);
    }
}
