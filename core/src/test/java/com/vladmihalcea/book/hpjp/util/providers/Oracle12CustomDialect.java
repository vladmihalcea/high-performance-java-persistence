package com.vladmihalcea.book.hpjp.util.providers;

import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.SQL2008StandardLimitHandler;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

/***
 * @author Vlad Mihalcea
 */
public class Oracle12CustomDialect extends Oracle12cDialect {

    @Override
    public SequenceInformationExtractor getSequenceInformationExtractor() {
        return SequenceInformationExtractorNoOpImpl.INSTANCE;
    }

    @Override
    public LimitHandler getLimitHandler() {
        return SQL2008StandardLimitHandler.INSTANCE;
    }
}
