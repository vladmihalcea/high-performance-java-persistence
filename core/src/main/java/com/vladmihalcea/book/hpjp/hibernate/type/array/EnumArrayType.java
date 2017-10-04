package com.vladmihalcea.book.hpjp.hibernate.type.array;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.usertype.DynamicParameterizedType;

import java.util.Properties;

/**
 * @author Vlad MIhalcea
 */
public class EnumArrayType
		extends AbstractSingleColumnStandardBasicType<Enum[]>
		implements DynamicParameterizedType {

	public EnumArrayType() {
		super( ArraySqlTypeDescriptor.INSTANCE, EnumArrayTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "enum-array";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public void setParameterValues(Properties parameters) {
		((EnumArrayTypeDescriptor) getJavaTypeDescriptor()).setParameterValues(parameters);
	}
}