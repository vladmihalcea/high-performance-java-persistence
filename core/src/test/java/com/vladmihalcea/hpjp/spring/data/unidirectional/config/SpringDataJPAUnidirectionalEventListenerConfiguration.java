package com.vladmihalcea.hpjp.spring.data.unidirectional.config;

import com.vladmihalcea.hpjp.spring.data.unidirectional.event.CascadeDeleteEventListenerIntegrator;
import io.hypersistence.utils.spring.repository.BaseJpaRepositoryImpl;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;
import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
@ComponentScan(
    basePackages = {
        "com.vladmihalcea.hpjp.spring.data.unidirectional"
    }
)
@EnableJpaRepositories(
    basePackages = "com.vladmihalcea.hpjp.spring.data.unidirectional.repository",
    repositoryBaseClass = BaseJpaRepositoryImpl.class
)
public class SpringDataJPAUnidirectionalEventListenerConfiguration extends SpringDataJPAUnidirectionalConfiguration {

    @Override
    protected void additionalProperties(Properties properties) {
        super.additionalProperties(properties);
        properties.put(
            EntityManagerFactoryBuilderImpl.INTEGRATOR_PROVIDER,
            (IntegratorProvider) () -> List.of(
                CascadeDeleteEventListenerIntegrator.INSTANCE
            )
        );
    }
}
