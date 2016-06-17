package com.vladmihalcea.book.hpjp.hibernate.type.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

import java.io.IOException;

/**
 * @author Vlad Mihalcea
 */
public class JsonJavaTypeDescriptor extends AbstractTypeDescriptor<ObjectNode> {

    public static final JsonJavaTypeDescriptor INSTANCE = new JsonJavaTypeDescriptor();

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public JsonJavaTypeDescriptor() {
        super( ObjectNode.class );
    }

    @Override
    public String toString(ObjectNode value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("The given Json object value: " + value + " cannot be transformed to a String");
        }
    }

    @Override
    public ObjectNode fromString(String string) {
        try {
            return (ObjectNode) OBJECT_MAPPER.readTree(string);
        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value: " + string + " cannot be transformed to Json object");
        }
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public <X> X unwrap(ObjectNode value, Class<X> type, WrapperOptions options) {
        if ( value == null ) {
            return null;
        }
        if ( ObjectNode.class.isAssignableFrom( type ) ) {
            return (X) fromString(value.toString());
        }
        if ( String.class.isAssignableFrom( type ) ) {
            return (X) toString( value );
        }
        throw unknownUnwrap( type );
    }
    @Override
    public <X> ObjectNode wrap(X value, WrapperOptions options) {
        if ( value == null ) {
            return null;
        }
        return fromString(value.toString());
    }

}
