package com.vladmihalcea.book.hpjp.hibernate.identifier.tsid.generator;

import com.github.f4b6a3.tsid.TsidCreator;
import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * @author Vlad Mihalcea
 */
@IdGeneratorType( TsidGenerator.class )
@Retention(RetentionPolicy.RUNTIME)
@Target({ FIELD, METHOD })
public @interface Tsid {

    enum Generator {
        _256 {
            @Override
            com.github.f4b6a3.tsid.Tsid random() {
                return TsidCreator.getTsid256();
            }
        },
        _1024{
            @Override
            com.github.f4b6a3.tsid.Tsid random() {
                return TsidCreator.getTsid1024();
            }
        },
        _4096{
            @Override
            com.github.f4b6a3.tsid.Tsid random() {
                return TsidCreator.getTsid4096();
            }
        };

        abstract com.github.f4b6a3.tsid.Tsid random();
    }

    Generator generator() default Generator._256;
}
