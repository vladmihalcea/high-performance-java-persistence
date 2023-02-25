package com.vladmihalcea.book.hpjp.spring.transaction.contract;

import com.vladmihalcea.book.hpjp.spring.transaction.contract.config.ContractConfiguration;
import com.vladmihalcea.book.hpjp.spring.transaction.contract.domain.Annex;
import com.vladmihalcea.book.hpjp.spring.transaction.contract.domain.AnnexSignature;
import com.vladmihalcea.book.hpjp.spring.transaction.contract.domain.Contract;
import com.vladmihalcea.book.hpjp.spring.transaction.contract.domain.ContractSignature;
import com.vladmihalcea.book.hpjp.spring.transaction.contract.repository.ContractRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ContractConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ContractTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ContractRepository contractRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Before
    public void init() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                Contract contract = new Contract()
                    .setId(1L)
                    .setTitle("Hypersistence Training");

                ContractSignature contractSignature = new ContractSignature()
                    .setContract(contract)
                    .setFirstName("Vlad")
                    .setLastName("Mihalcea");
                
                Annex annex1 = new Annex()
                    .setId(1L)
                    .setDetails("High-Performance Java Persistence Training")
                    .setContract(contract);

                AnnexSignature annexSignature1 = new AnnexSignature()
                    .setAnnex(annex1)
                    .setFirstName("Vlad")
                    .setLastName("Mihalcea");

                Annex annex2 = new Annex()
                    .setId(2L)
                    .setDetails("High-Performance SQL Training")
                    .setContract(contract);

                AnnexSignature annexSignature2 = new AnnexSignature()
                    .setAnnex(annex2)
                    .setFirstName("Vlad")
                    .setLastName("Mihalcea");

                entityManager.persist(contract);
                entityManager.persist(contractSignature);
                entityManager.persist(annex1);
                entityManager.persist(annex2);
                entityManager.persist(annexSignature1);
                entityManager.persist(annexSignature2);
                
                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void test() {
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            AnnexSignature signature = entityManager.createQuery("""
                select a_s
                from AnnexSignature a_s
                join fetch a_s.annex a
                join fetch a.contract c
                where a_s.id = :id
                """, AnnexSignature.class)
            .setParameter("id", 2L)
            .getSingleResult();

            signature.setFirstName("Vlad-Alexandru");
            
            return null;
        });

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Annex annex = entityManager.createQuery("""
                select a
                from Annex a
                join fetch a.contract
                where a.id = :id
                """, Annex.class)
            .setParameter("id", 2L)
            .getSingleResult();

            annex.setDetails("High-Performance SQL Online Training");
            
            return null;
        });

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            entityManager.persist(
                new Annex()
                    .setId(3L)
                    .setDetails("Spring 6 Migration Training")
                    .setContract(
                        entityManager.getReference(
                            Contract.class, 1L
                        )
                    )
            );
            
            return null;
        });

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Annex annex = entityManager.getReference(Annex.class, 3L);
            entityManager.remove(annex);
            
            return null;
        });
    }
}
