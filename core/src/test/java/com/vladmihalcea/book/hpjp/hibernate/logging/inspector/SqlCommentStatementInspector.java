package com.vladmihalcea.book.hpjp.hibernate.logging.inspector;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vlad Mihalcea
 */
public class SqlCommentStatementInspector implements StatementInspector {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlCommentStatementInspector.class);

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
