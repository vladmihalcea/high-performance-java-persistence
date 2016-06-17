/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package com.vladmihalcea.book.hpjp.hibernate.type.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;

/**
 * Descriptor for a Json type.
 *
 * @author Vlad MIhalcea
 *
 */
public class PostgreSQLJsonType extends AbstractSingleColumnStandardBasicType<ObjectNode> {

	public static final PostgreSQLJsonType INSTANCE = new PostgreSQLJsonType();

	public PostgreSQLJsonType() {
		super( JsonObjectSqlTypeDescriptor.INSTANCE, JsonJavaTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "json";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

}