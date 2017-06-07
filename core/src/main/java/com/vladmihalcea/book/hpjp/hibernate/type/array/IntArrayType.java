package com.vladmihalcea.book.hpjp.hibernate.type.array;

import java.util.Properties;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.usertype.DynamicParameterizedType;

/**
 * @author Vlad MIhalcea
 */
public class IntArrayType
		extends AbstractSingleColumnStandardBasicType<int[]>
		implements DynamicParameterizedType {

	public IntArrayType() {
		super( ArraySqlTypeDescriptor.INSTANCE, IntArrayTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "int-array";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public void setParameterValues(Properties parameters) {
		((IntArrayTypeDescriptor) getJavaTypeDescriptor()).setParameterValues(parameters);
	}
}