package com.vladmihalcea.hpjp.hibernate.logging;

import com.vladmihalcea.hpjp.util.StackTraceUtils;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vlad Mihalcea
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
