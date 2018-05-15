package com.sorinc.test.hibernate.one2many;


import com.sorinc.test.domain.Account;
import com.sorinc.test.domain.Address;
import com.sorinc.test.domain.Credentials;
import com.sorinc.test.domain.Token;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

public class AccountRepositoryTest extends JpaHibernateTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    public void test_find_acc_by_credentials() {
        Optional<Account> accountOptional = accountRepository.findAccountBy(new Credentials("username_1", "password_1"));
        assertTrue(accountOptional.isPresent());
        assertEquals(accountOptional.get().getCredentials().getUsername(), "username_1");
        assertNull(accountOptional.get().getCredentials().getPassword());
    }

    @Test
    public void test_find_acc_by_token_reference() {
        assertTrue(true);
    }


    /**
     * fixme: AICI CRAPA PE saveAccount(account).Eu zic ca sigur nu am setat bine sequance pt id.
     * <p>
     *     ERROR MSG:
     *
     *
     21:24:03.724 [main] DEBUG o.s.t.a.AnnotationTransactionAttributeSource - Adding transactional method 'ro.hpm.hcs.aaa.provider.persistence.AccountRepositoryImpl.saveAccount' with attribute: PROPAGATION_REQUIRED,ISOLATION_DEFAULT; ''
     21:24:03.730 [main] DEBUG o.s.b.f.s.DefaultListableBeanFactory - Returning cached instance of singleton bean 'transactionManager'
     21:24:03.742 [main] DEBUG o.s.orm.jpa.JpaTransactionManager - Creating new transaction with name [ro.hpm.hcs.aaa.provider.persistence.AccountRepositoryImpl.saveAccount]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT; ''
     21:24:03.745 [main] DEBUG o.s.orm.jpa.JpaTransactionManager - Opened new EntityManager [SessionImpl(PersistenceContext[entityKeys=[],collectionKeys=[]];ActionQueue[insertions=ExecutableList{size=0} updates=ExecutableList{size=0} deletions=ExecutableList{size=0} orphanRemovals=ExecutableList{size=0} collectionCreations=ExecutableList{size=0} collectionRemovals=ExecutableList{size=0} collectionUpdates=ExecutableList{size=0} collectionQueuedOps=ExecutableList{size=0} unresolvedInsertDependencies=null])] for JPA transaction
     21:24:03.752 [main] DEBUG o.h.e.t.internal.TransactionImpl - begin
     21:24:03.795 [main] DEBUG org.hibernate.engine.spi.ActionQueue - Executing identity-insert immediately

     21:24:03.802 [main] DEBUG org.hibernate.SQL -
     insert
     into
     account
     (id, created_at, updated_at, country, county, email, first_name, last_name, mid_name, password, phone, street, town, type, username, zip_code)
     values
     (null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
     Hibernate:
     insert
     into
     account
     (id, created_at, updated_at, country, county, email, first_name, last_name, mid_name, password, phone, street, town, type, username, zip_code)
     values
     (null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)

     21:24:03.809 [main] DEBUG o.h.id.IdentifierGeneratorHelper - Natively generated identity: 3
     21:24:03.810 [main] DEBUG o.h.r.j.i.ResourceRegistryStandardImpl - HHH000387: ResultSet's statement was not registered
     21:24:03.817 [main] DEBUG org.hibernate.engine.spi.ActionQueue - Executing identity-insert immediately

     21:24:03.817 [main] DEBUG org.hibernate.SQL -
     insert
     into
     token
     (id, created_at, updated_at, account_id, jwt, reference)
     values
     (null, ?, ?, ?, ?, ?)
     Hibernate:
     insert
     into
     token
     (id, created_at, updated_at, account_id, jwt, reference)
     values
     (null, ?, ?, ?, ?, ?)

     21:24:03.835 [main] DEBUG o.h.e.jdbc.spi.SqlExceptionHelper - could not execute statement [n/a]
     org.h2.jdbc.JdbcSQLException: NULL not allowed for column "ACCOUNT_ID"; SQL statement:
     insert into token (id, created_at, updated_at, account_id, jwt, reference) values (null, ?, ?, ?, ?, ?) [23502-191]
     at org.h2.message.DbException.getJdbcSQLException(DbException.java:345) ~[h2-1.4.191.jar:1.4.191]
     *
     *
     * </p>
     */

    @Test
    public void test_create_new_acc() {
        Account a = build_account_with_2_tokens();
        Account saveAccount = accountRepository.saveAccount(a);
        assertNotNull(saveAccount);
    }

    private Account build_account_with_2_tokens() {
        Set<Token> tokens = new HashSet<>();
        for (int i = 0; i < 2; i++)
            tokens.add(build_token());
        return build_account(tokens);
    }

    private Account build_account(Set<Token> tokens) {
        return Account.builder()
                .address(Address.builder()
                        .country("country")
                        .county("county")
                        .street("street")
                        .town("town")
                        .zipCode("4000340")
                        .build())
                .credentials(Credentials.builder()
                        .username("username")
                        .password("password")
                        .build())
                .firstName("frst_name")
                .lastName("lst_name")
                .email("fake_acc@email.net")
                .phone("00755678900")
                .type("business")
                .tokens(tokens == null ? new HashSet<>() : tokens)
                .build();
    }

    private Token build_token() {
        return Token.builder()
                .reference(build_reference())
                .encodedValue(build_jwt())
                .build();
    }

    private String build_jwt() {
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwianRpIjoiMTIzNDU2Nzg5OSIsImlzcyI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzk5OTl9.M8ql9mtklYQKkY64jcxMuGXHlAjGMEHrXDLwbitWFTU";
    }

    private UUID build_reference() {
        return UUID.randomUUID();
    }
}
