package com.vladmihalcea.hpjp.hibernate.identifier.string;

import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * The {@link com.vladmihalcea.hpjp.hibernate.identifier.string.StringSequence}
 *
 * @author Vlad Mihalcea
 * @since 1.x.y
 */
@IdGeneratorType(StringSequenceIdentifierGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD, METHOD})
public @interface StringSequence {
    String sequenceName();
    int initialValue() default 1;
    int incrementSize() default 1;
    String sequencePrefix();
}
