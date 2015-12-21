package com.vladmihalcea.guide.collection.type;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

import java.util.List;

/**
 * <code>CommaDelimitedStringsType</code> - Comma delimited strings
 *
 * @author Vlad Mihalcea
 */
public class CommaDelimitedStringsType extends AbstractSingleColumnStandardBasicType<List> {

    public CommaDelimitedStringsType() {
        super(
            VarcharTypeDescriptor.INSTANCE,
            new CommaDelimitedStringsJavaTypeDescriptor()
        );
    }

    @Override
    public String getName() {
        return "comma_delimited_strings";
    }
}
