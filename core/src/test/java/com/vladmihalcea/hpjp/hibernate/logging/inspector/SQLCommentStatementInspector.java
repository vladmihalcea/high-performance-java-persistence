package com.vladmihalcea.hpjp.hibernate.logging.inspector;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * @author Vlad Mihalcea
 */
public class SQLCommentStatementInspector implements StatementInspector {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLCommentStatementInspector.class);

    private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile("\\/\\*.*?\\*\\/\\s*");

    @Override
    public String inspect(String sql) {
        LOGGER.debug(
            "Executing SQL query: {}",
            sql
        );

        return SQL_COMMENT_PATTERN.matcher(sql).replaceAll("");
    }
}
