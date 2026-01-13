package com.vladmihalcea.hpjp.hibernate.identifier.string;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.Configurable;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.util.Properties;


/**
 * @author Vlad Mihalcea
 */
public class StringSequenceIdentifierGenerator extends SequenceStyleGenerator implements Configurable {

    public static final String SEQUENCE_PREFIX = "sequence_prefix";

    private final String sequenceName;
    private final int initialValue;
    private final int incrementSize;

    private String sequencePrefix;
    private String sequenceCallSyntax;

    public StringSequenceIdentifierGenerator(
        StringSequence annotation,
        Member idMember,
        GeneratorCreationContext creationContext) {
        sequenceName = annotation.sequenceName();
        initialValue = annotation.initialValue();
        incrementSize = annotation.incrementSize();

        sequencePrefix = annotation.sequencePrefix();
    }

    @Override
    public void configure(GeneratorCreationContext creationContext, Properties params) throws MappingException {
        params.setProperty(SEQUENCE_PARAM, sequenceName);
        params.setProperty(INITIAL_PARAM, String.valueOf(initialValue));
        params.setProperty(INCREMENT_PARAM, String.valueOf(incrementSize));

        super.configure(creationContext, params);
        final ServiceRegistry serviceRegistry = creationContext.getServiceRegistry();

        final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService(
            JdbcEnvironment.class
        );

        final Dialect dialect = jdbcEnvironment.getDialect();

        final String sequencePerEntitySuffix = ConfigurationHelper.getString(
            SequenceStyleGenerator.CONFIG_SEQUENCE_PER_ENTITY_SUFFIX,
            params,
            SequenceStyleGenerator.DEF_SEQUENCE_SUFFIX
        );

        final String defaultSequenceName = params.getProperty(JPA_ENTITY_NAME) + sequencePerEntitySuffix;

        sequenceCallSyntax = dialect.getSequenceSupport().getSequenceNextValString(
            ConfigurationHelper.getString(
                SequenceStyleGenerator.SEQUENCE_PARAM,
                params,
                defaultSequenceName
            )
        );
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object obj) {
        long seqValue = ((Number)
            session.createNativeQuery(sequenceCallSyntax).uniqueResult()
        ).longValue();

        return sequencePrefix + String.format("%011d%s", 0, seqValue);
    }
}
