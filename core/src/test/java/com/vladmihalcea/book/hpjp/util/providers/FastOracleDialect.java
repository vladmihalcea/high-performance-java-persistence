package com.vladmihalcea.book.hpjp.util.providers;

import org.hibernate.dialect.OracleDialect;
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
}
