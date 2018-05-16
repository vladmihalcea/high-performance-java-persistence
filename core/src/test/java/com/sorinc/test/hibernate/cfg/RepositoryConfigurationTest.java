package com.sorinc.test.hibernate.cfg;

import com.sorinc.test.hibernate.one2many.AccountRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import javax.persistence.EntityManagerFactory;

@Configuration
public class RepositoryConfigurationTest {

//    @PersistenceContext
//    protected EntityManager em;

    @Bean
    public AccountRepository accountRepository(EntityManagerFactory entityManagerFactory) {
        return new AccountRepositoryImpl(entityManagerFactory.createEntityManager());
    }


}
