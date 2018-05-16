package com.sorinc.test.domain;


import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Builder
public class Account {

    @Getter
    private final String firstName, lastName, midName, email, phone, type;

    @Getter
    private final Credentials credentials;

    @Getter
    private final Address address;

    @Getter
    private final Set<Token> tokens;


    public boolean equals(Object o) {
        if (this == o) return true;
        boolean isAccountType = o instanceof Account;
        return isAccountType && this.email.equals(((Account) o).getEmail());
    }

    public Optional<Token> findToken(String tokenReference) {
        log.trace("search for token with reference: {}", tokenReference);
        return tokens.stream().filter(t -> t.hasTheSame(tokenReference)).findFirst();
    }

}
