package com.vladmihalcea.hpjp.spring.data.recursive.config;

import com.vladmihalcea.hpjp.spring.data.base.config.SpringDataJPABaseConfiguration;
import com.vladmihalcea.hpjp.spring.data.recursive.domain.Post;
import com.vladmihalcea.hpjp.spring.data.recursive.domain.PostCommentDTO;
import io.hypersistence.utils.hibernate.type.util.ClassImportIntegrator;
import io.hypersistence.utils.spring.repository.BaseJpaRepositoryImpl;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
@ComponentScan(
    basePackages = {
        "com.vladmihalcea.hpjp.spring.data.recursive",
    }
)
@EnableJpaRepositories(
    value = "com.vladmihalcea.hpjp.spring.data.recursive.repository",
    repositoryBaseClass = BaseJpaRepositoryImpl.class
)
public class SpringDataJPARecursiveConfiguration extends SpringDataJPABaseConfiguration {

    @Override
    protected String packageToScan() {
        return Post.class.getPackageName();
    }

    @Override
    protected void additionalProperties(Properties properties) {
        super.additionalProperties(properties);
        properties.put("hibernate.jdbc.batch_size", "100");
        properties.put("hibernate.order_inserts", "true");
        properties.put(
            "hibernate.integrator_provider",
            (IntegratorProvider) () -> Collections.singletonList(
                new ClassImportIntegrator(
                    List.of(
                        PostCommentDTO.class
                    )
                )
            )
        );
    }
}
