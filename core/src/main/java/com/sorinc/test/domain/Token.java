package com.sorinc.test.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Builder
@RequiredArgsConstructor
public class Token {

    private static final Pattern JwT_Pattern = Pattern.compile("^(?<h>.+)\\.(?<p>.+)\\.(?<s>.+)$");

    @Getter
    private final UUID reference;
    @Getter
    private final String encodedValue;

    public JwTBody getJwtBody() {

        if (isValidateJwT()) {
            String jwtBody = getJwTBody();
            String jwtBodyJson = decodeJwtBody(jwtBody);
            try {
                JwTBody body = new ObjectMapper().readValue(jwtBodyJson, JwTBody.class);
                log.debug("{}", body);

                return body;
            } catch (IOException e) {
                throw new AAAException("parse of jwt payload failed", e);
            }
        }

        throw new AAAException("JsonWebToken is not conform with standard structure");
    }

    public boolean hasTheSame(String reference){
        return this.reference.equals(UUID.fromString(reference));
    }

    private boolean isValidateJwT() {
        Matcher m = JwT_Pattern.matcher(encodedValue);
        return m.find();
    }

    private String getJwTBody() {
        Matcher m = JwT_Pattern.matcher(encodedValue);
        return m.find() ? m.group("p") : "";
    }

    /**
     * <p>
     * - https://stackoverflow.com/questions/46240495/base64-encoding-illegal-base64-character-3c
     * - https://stackoverflow.com/questions/19743851/base64-java-encode-and-decode-a-string
     * </p>
     *
     * @param encodedBody: jwt payload that is encoded base64Url
     * @return: jwt payload decoded
     */
    private String decodeJwtBody(String encodedBody) {
        byte[] a = Base64.getUrlDecoder().decode(encodedBody);
        return new String(a);
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        boolean isTokenInstance = o instanceof Token;
        return isTokenInstance && this.reference.equals(((Token)o).getReference()) ;
    }

}
