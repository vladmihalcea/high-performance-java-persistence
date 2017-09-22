package com.vladmihalcea.book.hpjp.hibernate.identifier.batch.jta;

import com.vladmihalcea.book.hpjp.util.spring.config.jta.PostgreSQLJTATransactionManagerConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JTATableIdentifierTestConfiguration extends PostgreSQLJTATransactionManagerConfiguration {

    @Override
    protected Class configurationClass() {
        return Post.class;
    }
}
