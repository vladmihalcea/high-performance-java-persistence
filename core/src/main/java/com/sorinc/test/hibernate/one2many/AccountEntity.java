package com.sorinc.test.hibernate.one2many;

import com.sorinc.test.domain.Account;
import com.sorinc.test.domain.Address;
import com.sorinc.test.domain.Credentials;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;

import javax.persistence.*;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@ToString(exclude = {"password", "tokens"})
@Entity
@Table(name = "account")
public class AccountEntity extends BaseEntity<Account> {

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "mid_name")
    private String midName;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password; //password hashed with BCrypt !!!

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "country")
    private String country;

    @Column(name = "county")
    private String county;

    @Column(name = "town")
    private String town;

    @Column(name = "street")
    private String street;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "type")
    private String type;

    @OneToMany(
            mappedBy = "account",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<TokenEntity> tokens;


    public AccountEntity() {

    }

    public AccountEntity(Account account) {
        this.firstName = account.getFirstName();
        this.lastName = account.getLastName();
        this.midName = account.getMidName();
        this.username = account.getCredentials().getUsername();
        this.password = account.getCredentials().getPassword();
        this.email = account.getEmail();
        this.phone = account.getPhone();
        this.country = account.getAddress().getCountry();
        this.county = account.getAddress().getCounty();
        this.town = account.getAddress().getTown();
        this.street = account.getAddress().getStreet();
        this.zipCode = account.getAddress().getZipCode();
        this.type = account.getType();
        this.tokens = account.getTokens().stream().map(TokenEntity::new).collect(Collectors.toSet());
    }

    private String hash_password(String password_as_plain_text) {
        String salt = BCrypt.gensalt(12);
        return BCrypt.hashpw(password_as_plain_text, salt);
    }

//    public boolean check_password(String received_plain_text_password) {
//        if (this.is_not_a_valid_password_hash())
//            throw new java.lang.IllegalArgumentException("Invalid hash provided for comparison");
//        return BCrypt.checkpw(received_plain_text_password, this.password);
//    }

    @Override
    public Account toBusinessObject() {

        return Account.builder()
                .firstName(firstName)
                .lastName(lastName)
                .midName(midName)
                .email(email)
                .phone(phone)
                .credentials(Credentials.builder()
                        .username(username)
                        .build())
                .address(Address.builder()
                        .country(country)
                        .county(county)
                        .town(town)
                        .street(street)
                        .zipCode(zipCode)
                        .build())
                .type(type)
                .tokens(tokens.stream()
                        .map(TokenEntity::toBusinessObject)
                        .collect(Collectors.toSet()))
                .build();
    }

    private boolean is_not_a_valid_password_hash() {
        return this.password == null || !this.password.startsWith("$2a$");
    }
}
