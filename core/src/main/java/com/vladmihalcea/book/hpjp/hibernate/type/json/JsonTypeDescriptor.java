package com.vladmihalcea.book.hpjp.hibernate.type.json;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.usertype.DynamicParameterizedType;

import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class JsonTypeDescriptor
        extends AbstractTypeDescriptor<Object> implements DynamicParameterizedType {

    private Class<?> jsonObjectClass;

    @Override
    public void setParameterValues(Properties parameters) {
        jsonObjectClass = ( (ParameterType) parameters.get( PARAMETER_TYPE ) ).getReturnedClass();

    }

    public JsonTypeDescriptor() {
        super( Object.class, new MutableMutabilityPlan<Object>() {
            @Override
            protected Object deepCopyNotNull(Object value) {
                return JacksonUtil.clone(value);
            }
        });
    }

    @Override
    public boolean areEqual(Object one, Object another) {
        if ( one == another ) {
            return true;
        }
        if ( one == null || another == null ) {
            return false;
        }
        if ( one instanceof String && another instanceof String ) {
            return one.equals(another);
        }
        return JacksonUtil.toJsonNode(JacksonUtil.toString(one)).equals(
                JacksonUtil.toJsonNode(JacksonUtil.toString(another)));
    }

    @Override
    public String toString(Object value) {
        return JacksonUtil.toString(value);
    }

    @Override
    public Object fromString(String string) {
        return JacksonUtil.fromString(string, jsonObjectClass);
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public <X> X unwrap(Object value, Class<X> type, WrapperOptions options) {
        if ( value == null ) {
            return null;
        }
        if ( String.class.isAssignableFrom( type ) ) {
            return (X) toString(value);
        }
        if ( Object.class.isAssignableFrom( type ) ) {
            return (X) JacksonUtil.toJsonNode(toString(value));
        }
        throw unknownUnwrap( type );
    }

    @Override
    public <X> Object wrap(X value, WrapperOptions options) {
        if ( value == null ) {
            return null;
        }
        return fromString(value.toString());
    }

}
