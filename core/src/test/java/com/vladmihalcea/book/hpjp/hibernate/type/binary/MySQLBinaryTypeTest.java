package com.vladmihalcea.book.hpjp.hibernate.type.binary;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Vlad Mihalcea
 */
public class MySQLBinaryTypeTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            User.class
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Test
    public void test() {
        String password = "my_secret_password";

        doInJPA(entityManager -> {
            entityManager.persist(
                new User()
                    .setUserName("vladmihalcea")
                    .setPassword(hash(password))
            );
        });

        doInJPA(entityManager -> {
            User user = entityManager.unwrap(Session.class)
                .bySimpleNaturalId(User.class)
                .load("vladmihalcea");

            assertArrayEquals(hash(password), user.getPassword());
        });
    }

    public byte[] hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes());
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Entity(name = "User")
    @Table(name = "`user`")
    public static class User {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @NaturalId
        private String userName;

        @Column(columnDefinition = "BINARY(16)")
        private byte[] password;

        public Long getId() {
            return id;
        }

        public User setId(Long id) {
            this.id = id;
            return this;
        }

        public String getUserName() {
            return userName;
        }

        public User setUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public byte[] getPassword() {
            return password;
        }

        public User setPassword(byte[] password) {
            this.password = password;
            return this;
        }
    }
}