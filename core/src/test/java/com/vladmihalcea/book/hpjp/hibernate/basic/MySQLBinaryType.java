package com.vladmihalcea.book.hpjp.hibernate.basic;


import org.hibernate.type.BinaryType;
import org.hibernate.type.descriptor.sql.BinaryTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
public class MySQLBinaryType extends BinaryType {

    public MySQLBinaryType() {
        setSqlTypeDescriptor(BinaryTypeDescriptor.INSTANCE);
    }
}
