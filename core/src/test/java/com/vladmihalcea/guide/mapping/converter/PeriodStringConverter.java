package com.vladmihalcea.guide.mapping.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.time.Period;

/**
 * <code>PeriodStringConverter</code> - Period String Converter
 *
 * @author Vlad Mihalcea
 */
@Converter
public class PeriodStringConverter implements AttributeConverter<Period, String> {

    @Override
    public String convertToDatabaseColumn(Period attribute) {
        return attribute.toString();
    }

    @Override
    public Period convertToEntityAttribute(String dbData) {
        return Period.parse(dbData);
    }
}
