package com.sorinc.test.hibernate.one2many;


import com.sorinc.test.domain.Account;
import com.sorinc.test.domain.Credentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Optional;


@Slf4j
public class AccountRepositoryImpl extends GenericRepository<AccountEntity> implements AccountRepository {

    public AccountRepositoryImpl(EntityManager em) {
        super(em);
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findAccountBy(Credentials credentials) {
        try {
            TypedQuery<AccountEntity> query = entityManager.createQuery("from AccountEntity ac where ac.username=:username and ac.password=:password", AccountEntity.class);
            query.setParameter("username", credentials.getUsername());
            query.setParameter("password", credentials.getPassword());
            AccountEntity r = query.getSingleResult();
            log.trace("found account: {}", r);

            return Optional.of(r.toBusinessObject());
        }catch (javax.persistence.NoResultException x){
            if (x.getMessage().equalsIgnoreCase("No entity found for query")){
                log.warn("no account found for username: {}", credentials.getUsername());
            }

            return Optional.empty() ;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findAccountBy(String tokenReference) {
        return Optional.empty();
    }

    @Override
    @Transactional
    public Account saveAccount(Account account) {
        AccountEntity accountEntity = new AccountEntity(account);
        log.trace("create new account: {}", accountEntity);
        AccountEntity save = save(accountEntity);

        return save.toBusinessObject();
    }

    @Override
    @Transactional
    public Token saveToken(Token token, Account account) {
        return null;
    }

    @Override
    @Transactional
    public Token invalidateToken(Token token) {

        return token;
    }

//    @Transactional
//    public Account save(Account account) {
//        AccountEntity entity = new AccountEntity(account);
//        try {
//            return this.save(entity).toBusinessObject();
//        } catch (PersistenceException e) {
//            throw new AAAException("Account already exists", e);
//        }
//    }
//
//    @Transactional
//    public Account update(Account account) {
//        this.merge(new AccountEntity(account));
//        return account;
//    }
//
//
//    @Transactional(readOnly = true)
//    public Optional<Account> getAccountFor(String email, String password) {
//        logger.info("Email {} password {}", email, password);
//        String queryString = "SELECT ac FROM AccountEntity ac WHERE ac.email=:email and ac.password_hash=:password";
//        Query query = em.createQuery(queryString);
//        query.setParameter("email", email);
//        query.setParameter("password", password);
//        List<AccountEntity> tokenEntities = query.getResultList();
//        if (!tokenEntities.isEmpty()) {
//            return Optional.of(tokenEntities.get(0).toBusinessObject());
//        }
//        return Optional.empty();
//    }
//
//
//    @Transactional(readOnly = true)
//    public Optional<Account> getAccountFor(UUID tokenReference) {
//        String queryString = "SELECT r FROM TokenEntity r WHERE r.referenceToken=:referenceToken";
//        Query query = em.createQuery(queryString);
//        query.setParameter("referenceToken", tokenReference);
//        List<TokenEntity> tokenEntities = query.getResultList();
//        if (!tokenEntities.isEmpty()) {
//            return Optional.of(tokenEntities.get(0).getAccount().toBusinessObject());
//        }
//        return Optional.empty();
//    }
}
