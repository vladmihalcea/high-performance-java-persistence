package com.vladmihalcea.book.hpjp.hibernate.logging.inspector;

import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class StatementInspectorSqlCommentTest extends SqlCommentTest {

    @Override
    protected void additionalProperties(Properties properties) {
        super.additionalProperties(properties);
        properties.put(
                "hibernate.session_factory.statement_inspector",
                "com.vladmihalcea.book.hpjp.hibernate.logging.inspector.SqlCommentStatementInspector"
        );
    }

}
