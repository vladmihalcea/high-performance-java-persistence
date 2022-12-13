package com.vladmihalcea.book.hpjp.util.providers;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.pagination.LegacyOracleLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

/***
 * @author Vlad Mihalcea
 */
public class FastOracleDialect extends OracleDialect {

    @Override
    public SequenceInformationExtractor getSequenceInformationExtractor() {
        return SequenceInformationExtractorNoOpImpl.INSTANCE;
    }

    @Override
    public boolean supportsFetchClause(FetchClauseType type) {
        return false;
    }

    @Override
    public LimitHandler getLimitHandler() {
        return new LegacyOracleLimitHandler(DatabaseVersion.make(11));
    }
}
