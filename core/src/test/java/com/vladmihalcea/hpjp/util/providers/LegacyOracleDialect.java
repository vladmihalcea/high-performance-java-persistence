package com.vladmihalcea.hpjp.util.providers;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.pagination.LegacyOracleLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

/***
 * @author Vlad Mihalcea
 */
public class LegacyOracleDialect extends OracleDialect {

    public LegacyOracleDialect() {
    }

    public LegacyOracleDialect(DatabaseVersion version) {
        super(version);
    }

    public LegacyOracleDialect(DialectResolutionInfo info) {
        super(info);
    }

    @Override
    public SequenceInformationExtractor getSequenceInformationExtractor() {
        return SequenceInformationExtractorNoOpImpl.INSTANCE;
    }

    @Override
    public LimitHandler getLimitHandler() {
        return new LegacyOracleLimitHandler(DatabaseVersion.make(21));
    }
}
