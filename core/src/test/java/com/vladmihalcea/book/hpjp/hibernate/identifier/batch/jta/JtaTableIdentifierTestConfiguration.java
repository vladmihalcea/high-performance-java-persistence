package com.vladmihalcea.book.hpjp.hibernate.identifier.batch.jta;

import com.vladmihalcea.book.hpjp.util.spring.config.jta.PostgreSQLJtaTransactionManagerConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JtaTableIdentifierTestConfiguration extends PostgreSQLJtaTransactionManagerConfiguration {

    @Override
    protected Class configurationClass() {
        return Post.class;
    }
}
