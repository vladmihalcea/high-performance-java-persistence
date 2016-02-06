package com.vladmihalcea.book.hpjp.hibernate.listener;

import java.util.Date;

/**
 * <code>Updatable</code> - Updatable
 *
 * @author Vlad Mihalcea
 */
public interface Updatable {

    void setTimestamp(Date timestamp);

    Date getTimestamp();
}
