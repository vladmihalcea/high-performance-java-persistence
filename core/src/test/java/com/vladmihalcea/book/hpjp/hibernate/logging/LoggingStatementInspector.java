package com.vladmihalcea.book.hpjp.hibernate.logging;

import com.vladmihalcea.book.hpjp.util.StackTraceUtils;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link com.vladmihalcea.book.hpjp.hibernate.logging.LoggingStatementInspector}
 *
 * @author Vlad Mihalcea
 * @since 1.x.y
 */
public class LoggingStatementInspector implements StatementInspector {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingStatementInspector.class);

    private final String packageNamePrefix;

    public LoggingStatementInspector(String packageNamePrefix) {
        this.packageNamePrefix = packageNamePrefix;
    }

    @Override
    public String inspect(String sql) {
        LOGGER.info(
                "Executing SQL query: {} from {}",
                sql,
                StackTraceUtils.stackTracePath(
                    StackTraceUtils.stackTraceElements(
                        packageNamePrefix
                    )
                )
        );
        return null;
    }
}
