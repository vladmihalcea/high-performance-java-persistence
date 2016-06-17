package com.vladmihalcea.book.hpjp.hibernate.type.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

import java.io.IOException;

/**
 * @author Vlad Mihalcea
 */
public class JsonNodeJavaTypeDescriptor extends AbstractTypeDescriptor<JsonNode> {

    public static final JsonNodeJavaTypeDescriptor INSTANCE = new JsonNodeJavaTypeDescriptor();

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public JsonNodeJavaTypeDescriptor() {
        super( JsonNode.class );
    }

    @Override
    public String toString(JsonNode value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("The given JsonNode value: " + value + " cannot be transformed to a String");
        }
    }

    @Override
    public JsonNode fromString(String string) {
        try {
            return OBJECT_MAPPER.readTree(string);
        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value: " + string + " cannot be transformed to a JsonNode");
        }
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public <X> X unwrap(JsonNode value, Class<X> type, WrapperOptions options) {
        if ( value == null ) {
            return null;
        }
        if ( JsonNode.class.isAssignableFrom( type ) ) {
            return (X) value;
        }
        if ( ObjectNode.class.isAssignableFrom( type ) ) {
            return (X) fromString(type.toString());
        }
        if ( String.class.isAssignableFrom( type ) ) {
            return (X) toString( value );
        }
        throw unknownUnwrap( type );
    }
    @Override
    public <X> JsonNode wrap(X value, WrapperOptions options) {
        if ( value == null ) {
            return null;
        }
        return fromString(value.toString());
    }

}
