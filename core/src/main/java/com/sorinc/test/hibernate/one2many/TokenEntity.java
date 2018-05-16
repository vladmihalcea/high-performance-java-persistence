package com.sorinc.test.hibernate.one2many;


import com.sorinc.test.domain.Token;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


import javax.persistence.*;
import java.util.UUID;

@Slf4j
@ToString(exclude = {"jwt"})
@Entity
@Table(name = "token")
public class TokenEntity extends BaseEntity<Token> {

    //@Column(name = "jwt", columnDefinition = "BINARY(16)")
    @Column(name = "jwt")
    private String jwt;

    //@Column(name = "reference", columnDefinition = "BINARY(16)")
    @Column(name = "reference")
    private String reference;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    public TokenEntity() {
    }

    TokenEntity(Token token) {
        this.reference = token.getReference().toString();
        this.jwt = token.getEncodedValue();
    }

    @Override
    public Token toBusinessObject() {
        return Token.builder()
                .reference(UUID.fromString(reference))
                .encodedValue(jwt)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenEntity)) return false;
        return this.getId() != null && this.getId().equals(((TokenEntity) o).getId());
    }

    @Override
    public int hashCode() {
        return 31;
    }

}
