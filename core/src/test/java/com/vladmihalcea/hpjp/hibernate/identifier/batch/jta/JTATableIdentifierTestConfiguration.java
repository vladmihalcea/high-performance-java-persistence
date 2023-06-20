package com.vladmihalcea.hpjp.hibernate.identifier.batch.jta;

import com.vladmihalcea.hpjp.util.spring.config.jta.PostgreSQLJTATransactionManagerConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JTATableIdentifierTestConfiguration extends PostgreSQLJTATransactionManagerConfiguration {

    @Override
    protected Class configurationClass() {
        return Post.class;
    }
}
