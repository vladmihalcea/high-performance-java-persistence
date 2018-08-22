package com.vladmihalcea.book.hpjp.hibernate.type.json;

import org.hibernate.dialect.PostgreSQL95Dialect;

import java.sql.Types;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQL95JsonDialect extends PostgreSQL95Dialect {

	public PostgreSQL95JsonDialect() {
		super();
		this.registerHibernateType(Types.OTHER, JsonNodeBinaryType.class.getName());
	}
}
