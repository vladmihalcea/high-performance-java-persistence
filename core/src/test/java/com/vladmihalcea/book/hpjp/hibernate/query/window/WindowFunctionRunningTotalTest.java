package com.vladmihalcea.book.hpjp.hibernate.query.window;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.query.Query;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class WindowFunctionRunningTotalTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Account.class,
            AccountTransaction.class,
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    /**
     * account
     * -------
     *
     * | id | iban            | owner       |
     * |----|-----------------|-------------|
     * | 1  | 123-456-789-010 | Alice Smith |
     * | 2  | 123-456-789-101 | Bob Johnson |
     *
     * account_transaction
     * -------------------
     *
     * | id | amount | created_on          | account_id |
     * |----|--------|---------------------|------------|
     * | 1  | 2560   | 2019-10-13 12:23:00 | 1          |
     * | 2  | -200   | 2019-10-14 13:23:00 | 1          |
     * | 3  | 500    | 2019-10-14 15:45:00 | 1          |
     * | 4  | -1850  | 2019-10-15 10:15:00 | 1          |
     * | 5  | 2560   | 2019-10-13 15:23:00 | 2          |
     * | 6  | 300    | 2019-10-14 11:23:00 | 2          |
     * | 7  | -500   | 2019-10-14 14:45:00 | 2          |
     * | 8  | -150   | 2019-10-15 10:15:00 | 2          |
     */
    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            Account account1 = new Account()
                .setId(1L)
                .setOwner("Alice Smith")
                .setIban("123-456-789-010");

            Account account2 = new Account()
                .setId(2L)
                .setOwner("Bob Johnson")
                .setIban("123-456-789-101");

            entityManager.persist(account1);
            entityManager.persist(account2);

            entityManager.persist(
                new AccountTransaction()
                    .setId(1L)
                    .setAmount(2560L)
                    .setAccount(account1)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 13, 12, 23, 0)))
            );

            entityManager.persist(
                new AccountTransaction()
                    .setId(2L)
                    .setAmount(-200L)
                    .setAccount(account1)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 14, 13, 23, 0)))
            );

            entityManager.persist(
                new AccountTransaction()
                    .setId(3L)
                    .setAmount(500L)
                    .setAccount(account1)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 14, 15, 45, 0)))
            );

            entityManager.persist(
                new AccountTransaction()
                    .setId(4L)
                    .setAmount(-1850L)
                    .setAccount(account1)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 15, 10, 15, 0)))
            );

            entityManager.persist(
                new AccountTransaction()
                    .setId(5L)
                    .setAmount(2560L)
                    .setAccount(account2)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 13, 15, 23, 0)))
            );

            entityManager.persist(
                new AccountTransaction()
                    .setId(6L)
                    .setAmount(300L)
                    .setAccount(account2)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 14, 11, 23, 0)))
            );

            entityManager.persist(
                new AccountTransaction()
                    .setId(7L)
                    .setAmount(-500L)
                    .setAccount(account2)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 14, 14, 45, 0)))
            );

            entityManager.persist(
                new AccountTransaction()
                    .setId(8L)
                    .setAmount(-150L)
                    .setAccount(account2)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 15, 10, 15, 0)))
            );
        });
    }

    /**
     * Get the account transactions and calculate the current balance for each transaction.
     *
     * SELECT
     *    ROW_NUMBER() OVER(
     *       PARTITION BY account_id
     *       ORDER BY created_on, id
     *    ) AS nr,
     *    id,
     *    account_id,
     *    created_on,
     *    amount,
     *    SUM(amount) OVER(
     *        PARTITION BY account_id
     *        ORDER BY created_on, id
     *    ) AS balance
     * FROM account_transaction
     * ORDER BY id
     *
     * | nr | id | account_id | created_on                 | amount | balance |
     * |----|----|------------|----------------------------|--------|---------|
     * | 1  | 1  | 1          | 2019-10-13 12:23:00.000000 | 2560   | 2560    |
     * | 2  | 2  | 1          | 2019-10-14 13:23:00.000000 | -200   | 2360    |
     * | 3  | 3  | 1          | 2019-10-14 15:45:00.000000 | 500    | 2860    |
     * | 4  | 4  | 1          | 2019-10-15 10:15:00.000000 | -1850  | 1010    |
     * | 1  | 5  | 2          | 2019-10-13 15:23:00.000000 | 2560   | 2560    |
     * | 2  | 6  | 2          | 2019-10-14 11:23:00.000000 | 300    | 2860    |
     * | 3  | 7  | 2          | 2019-10-14 14:45:00.000000 | -500   | 2360    |
     * | 4  | 8  | 2          | 2019-10-15 10:15:00.000000 | -150   | 2210    |
     */
    @Test
    public void testSQL() {
        doInJPA(entityManager -> {
            List<Tuple> tuples = entityManager.createNativeQuery("""
                SELECT
                   ROW_NUMBER() OVER(
                      PARTITION BY account_id
                      ORDER BY created_on, id
                   ) AS nr,
                   id,
                   account_id,
                   created_on,
                   amount,
                   SUM(amount) OVER(       
                       PARTITION BY account_id
                       ORDER BY created_on, id  
                   ) AS balance
                FROM account_transaction
                ORDER BY id
			    """, Tuple.class)
            .getResultList();

            Tuple tuple1 = tuples.get(0);
            assertEquals(1L, longValue(tuple1.get("nr")));
            assertEquals(1L, longValue(tuple1.get("id")));
            assertEquals(2560L, longValue(tuple1.get("amount")));
            assertEquals(2560L, longValue(tuple1.get("balance")));
            assertEquals(1L, longValue(tuple1.get("account_id")));
            assertEquals(Timestamp.valueOf(LocalDateTime.of(2019, 10, 13, 12, 23, 0)), tuple1.get("created_on"));

            Tuple tuple2 = tuples.get(1);
            assertEquals(2L, longValue(tuple2.get("nr")));
            assertEquals(2L, longValue(tuple2.get("id")));
            assertEquals(-200L, longValue(tuple2.get("amount")));
            assertEquals(2360L, longValue(tuple2.get("balance")));
            assertEquals(1L, longValue(tuple2.get("account_id")));
            assertEquals(Timestamp.valueOf(LocalDateTime.of(2019, 10, 14, 13, 23, 0)), tuple2.get("created_on"));

            Tuple tuple3 = tuples.get(2);
            assertEquals(3L, longValue(tuple3.get("nr")));
            assertEquals(3L, longValue(tuple3.get("id")));
            assertEquals(500L, longValue(tuple3.get("amount")));
            assertEquals(2860L, longValue(tuple3.get("balance")));
            assertEquals(1L, longValue(tuple3.get("account_id")));
            assertEquals(Timestamp.valueOf(LocalDateTime.of(2019, 10, 14, 15, 45, 0)), tuple3.get("created_on"));

            Tuple tuple4 = tuples.get(3);
            assertEquals(4L, longValue(tuple4.get("nr")));
            assertEquals(4L, longValue(tuple4.get("id")));
            assertEquals(-1850L, longValue(tuple4.get("amount")));
            assertEquals(1010L, longValue(tuple4.get("balance")));
            assertEquals(1L, longValue(tuple4.get("account_id")));
            assertEquals(Timestamp.valueOf(LocalDateTime.of(2019, 10, 15, 10, 15, 0)), tuple4.get("created_on"));

            Tuple tuple5 = tuples.get(4);
            assertEquals(1L, longValue(tuple5.get("nr")));
            assertEquals(5L, longValue(tuple5.get("id")));
            assertEquals(2560L, longValue(tuple5.get("amount")));
            assertEquals(2560L, longValue(tuple5.get("balance")));
            assertEquals(2L, longValue(tuple5.get("account_id")));
            assertEquals(Timestamp.valueOf(LocalDateTime.of(2019, 10, 13, 15, 23, 0)), tuple5.get("created_on"));

            Tuple tuple6 = tuples.get(5);
            assertEquals(2L, longValue(tuple6.get("nr")));
            assertEquals(6L, longValue(tuple6.get("id")));
            assertEquals(300L, longValue(tuple6.get("amount")));
            assertEquals(2860L, longValue(tuple6.get("balance")));
            assertEquals(2L, longValue(tuple6.get("account_id")));
            assertEquals(Timestamp.valueOf(LocalDateTime.of(2019, 10, 14, 11, 23, 0)), tuple6.get("created_on"));

            Tuple tuple7 = tuples.get(6);
            assertEquals(3L, longValue(tuple7.get("nr")));
            assertEquals(7L, longValue(tuple7.get("id")));
            assertEquals(-500L, longValue(tuple7.get("amount")));
            assertEquals(2360L, longValue(tuple7.get("balance")));
            assertEquals(2L, longValue(tuple7.get("account_id")));
            assertEquals(Timestamp.valueOf(LocalDateTime.of(2019, 10, 14, 14, 45, 0)), tuple7.get("created_on"));

            Tuple tuple8 = tuples.get(7);
            assertEquals(4L, longValue(tuple8.get("nr")));
            assertEquals(8L, longValue(tuple8.get("id")));
            assertEquals(-150L, longValue(tuple8.get("amount")));
            assertEquals(2210L, longValue(tuple8.get("balance")));
            assertEquals(2L, longValue(tuple8.get("account_id")));
            assertEquals(Timestamp.valueOf(LocalDateTime.of(2019, 10, 15, 10, 15, 0)), tuple8.get("created_on"));
        });
    }

    @Test
    public void testJPQL() {
        doInJPA(entityManager -> {
            List<StatementRecord> records = entityManager.createQuery("""
                SELECT
                   ROW_NUMBER() OVER(       
                       PARTITION BY at.account.id
                       ORDER BY at.createdOn   
                   ) AS nr,
                   at,
                   SUM(at.amount) OVER(       
                       PARTITION BY at.account.id
                       ORDER BY at.createdOn   
                   ) AS balance
                FROM AccountTransaction at
                ORDER BY at.id
			    """, StatementRecord.class)
            .unwrap(Query.class)
            .setTupleTransformer((Object[] tuple, String[] aliases) -> new StatementRecord(
                longValue(tuple[0]),
                (AccountTransaction) tuple[1],
                longValue(tuple[2])
            ))
            .getResultList();

            assertEquals(8, records.size());

            StatementRecord record1 = records.get(0);
            assertEquals(1L, record1.nr().longValue());
            assertEquals(1L, record1.transaction().getId().longValue());
            assertEquals(1L, record1.transaction().getAccount().getId().longValue());
            assertEquals(2560L, record1.balance().longValue());

            StatementRecord record2 = records.get(1);
            assertEquals(2L, record2.nr().longValue());
            assertEquals(2L, longValue(record2.transaction().getId()));
            assertEquals(1L, longValue(record2.transaction().getAccount().getId()));
            assertEquals(2360L, longValue(record2.balance()));

            StatementRecord record3 = records.get(2);
            assertEquals(3L, record3.nr().longValue());
            assertEquals(3L, longValue(record3.transaction().getId()));
            assertEquals(1L, longValue(record3.transaction().getAccount().getId()));
            assertEquals(2860L, longValue(record3.balance()));

            StatementRecord record4 = records.get(3);
            assertEquals(4L, record4.nr().longValue());
            assertEquals(4L, longValue(record4.transaction().getId()));
            assertEquals(1L, longValue(record4.transaction().getAccount().getId()));
            assertEquals(1010L, longValue(record4.balance()));

            StatementRecord record5 = records.get(4);
            assertEquals(1L, record5.nr().longValue());
            assertEquals(5L, longValue(record5.transaction().getId()));
            assertEquals(2L, longValue(record5.transaction().getAccount().getId()));
            assertEquals(2560L, longValue(record5.balance()));
        });
    }

    public static record StatementRecord(
        Long nr,
        AccountTransaction transaction,
        Long balance
    ) {
    }

    @Entity(name = "Account")
    @Table(name = "account")
    public static class Account {

        @Id
        private Long id;

        private String owner;
        
        private String iban;

        public Long getId() {
            return id;
        }

        public Account setId(Long id) {
            this.id = id;
            return this;
        }

        public String getOwner() {
            return owner;
        }

        public Account setOwner(String owner) {
            this.owner = owner;
            return this;
        }

        public String getIban() {
            return iban;
        }

        public Account setIban(String iban) {
            this.iban = iban;
            return this;
        }
    }

    @Entity(name = "AccountTransaction")
    @Table(name = "account_transaction")
    public static class AccountTransaction {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Account account;

        private Long amount;

        @Temporal(TemporalType.TIMESTAMP)
        @Column(name = "created_on")
        private Date createdOn;

        public Long getId() {
            return id;
        }

        public AccountTransaction setId(Long id) {
            this.id = id;
            return this;
        }

        public Account getAccount() {
            return account;
        }

        public AccountTransaction setAccount(Account account) {
            this.account = account;
            return this;
        }

        public Long getAmount() {
            return amount;
        }

        public AccountTransaction setAmount(Long amount) {
            this.amount = amount;
            return this;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public AccountTransaction setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
            return this;
        }
    }
}
