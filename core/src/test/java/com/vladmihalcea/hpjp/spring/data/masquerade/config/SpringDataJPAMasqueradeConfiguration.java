package com.vladmihalcea.hpjp.spring.data.masquerade.config;

import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import com.vladmihalcea.hpjp.spring.data.base.config.SpringDataJPABaseConfiguration;
import com.vladmihalcea.hpjp.spring.data.masquerade.domain.Post;
import com.vladmihalcea.hpjp.spring.data.masquerade.dto.PostCommentDTO;
import io.hypersistence.utils.hibernate.type.util.ClassImportIntegrator;
import io.hypersistence.utils.spring.repository.BaseJpaRepositoryImpl;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.springframework.context.annotation.Bean;
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
        "com.vladmihalcea.hpjp.spring.data.masquerade",
    }
)
@EnableJpaRepositories(
    value = "com.vladmihalcea.hpjp.spring.data.masquerade.repository",
    repositoryBaseClass = BaseJpaRepositoryImpl.class
)
public class SpringDataJPAMasqueradeConfiguration extends SpringDataJPABaseConfiguration {

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

    @Bean
    public CriteriaBuilderFactory criteriaBuilderFactory(EntityManagerFactory entityManagerFactory) {
        CriteriaBuilderConfiguration config = Criteria.getDefault();
        return config.createCriteriaBuilderFactory(entityManagerFactory);
    }
}
