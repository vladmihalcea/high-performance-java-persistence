package com.vladmihalcea.hpjp.hibernate.logging.inspector;

import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class StatementInspectorSqlCommentTest extends SQLCommentTest {

    @Override
    protected void additionalProperties(Properties properties) {
        super.additionalProperties(properties);
        properties.put(
            "hibernate.session_factory.statement_inspector",
            SQLCommentStatementInspector.class.getName()
        );
    }

}
