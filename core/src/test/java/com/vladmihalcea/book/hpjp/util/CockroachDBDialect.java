package com.vladmihalcea.book.hpjp.util;

import java.sql.Types;

import org.hibernate.dialect.PostgreSQL82Dialect;

/**
 * @author Vlad Mihalcea
 */
public class CockroachDBDialect extends PostgreSQL82Dialect {

	public CockroachDBDialect() {
		super();
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "smallint" );
		registerColumnType( Types.INTEGER, "integer" );

		registerColumnType( Types.FLOAT, "double precision" );
		registerColumnType( Types.DOUBLE, "double precision" );

		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.OTHER, "interval" );
	}
}
