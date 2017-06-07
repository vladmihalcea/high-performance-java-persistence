package com.vladmihalcea.book.hpjp.hibernate.type.array;

import java.sql.Types;

import org.hibernate.dialect.PostgreSQL95Dialect;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQL95ArrayDialect extends PostgreSQL95Dialect {

	public PostgreSQL95ArrayDialect() {
		super();
		this.registerColumnType( Types.ARRAY, "array" );
	}
}
