package com.vladmihalcea.book.hpjp.hibernate.naming;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * <code>ExtendedNamingTest</code> - ExtendedNaming Test
 *
 * @author Vlad Mihalcea
 */
public class ExtendedNamingTest extends DefaultNamingTest {

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        //properties.put("")
        return properties;
    }
}
