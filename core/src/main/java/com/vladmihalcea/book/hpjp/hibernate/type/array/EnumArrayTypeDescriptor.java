package com.vladmihalcea.book.hpjp.hibernate.type.array;

/**
 * @author Vlad Mihalcea
 */
public class EnumArrayTypeDescriptor
		extends AbstractArrayTypeDescriptor<Enum[]> {

	public static final EnumArrayTypeDescriptor INSTANCE = new EnumArrayTypeDescriptor();

	public EnumArrayTypeDescriptor() {
		super( Enum[].class );
	}

	@Override
	protected String getSqlArrayType() {
		return "integer";
	}
}
