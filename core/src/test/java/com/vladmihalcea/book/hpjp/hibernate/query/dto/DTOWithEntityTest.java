package com.vladmihalcea.book.hpjp.hibernate.query.dto;

import java.util.List;

import org.hibernate.query.Query;
import org.hibernate.transform.ResultTransformer;

import org.junit.Assert;
import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractTest;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class DTOWithEntityTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Person.class,
                Country.class
        };
    }

    @Test
    public void test() {
        doInJPA( entityManager -> {
            Country usa = new Country();
            usa.setName( "USA" );
            usa.setLocale( "en-US" );
            entityManager.persist( usa );

            Country romania = new Country();
            romania.setName( "Romania" );
            romania.setLocale( "ro-RO" );
            entityManager.persist( romania );

            Person chris = new Person();
            chris.setName( "Chris" );
            chris.setLocale( "en-US" );
            entityManager.persist( chris );

            Person vlad = new Person();
            vlad.setName( "Vlad" );
            vlad.setLocale( "ro-RO" );
            entityManager.persist( vlad );
        } );

        doInJPA( entityManager -> {
            LOGGER.info( "Using constructor resultl set" );
            List<PersonAndCountryDTO> personAndAddressDTOs = entityManager.createQuery(
        "select new " +
                    "   com.vladmihalcea.book.hpjp.hibernate.query.dto.PersonAndCountryDTO(" +
                    "       p, " +
                    "       c.name" +
                    "   ) " +
                    "from Person p " +
                    "join Country c on p.locale = c.locale " +
                    "order by p.id", PersonAndCountryDTO.class)
            .getResultList();

            PersonAndCountryDTO firstEntry = personAndAddressDTOs.get( 0 );
            Assert.assertEquals( "Chris", firstEntry.getPerson().getName());
            assertEquals("USA", firstEntry.getCountry());
        } );

        doInJPA( entityManager -> {
            LOGGER.info( "Using ResultTransformer" );
            List<PersonAndCountryDTO> personAndAddressDTOs = entityManager.createQuery(
            "select p, c.name " +
                    "from Person p " +
                    "join Country c on p.locale = c.locale " +
                    "order by p.id")
            .unwrap( Query.class )
            .setResultTransformer( new ResultTransformer() {
                @Override
                public Object transformTuple(Object[] tuple, String[] aliases) {
                    return new PersonAndCountryDTO(
                        (Person) tuple[0],
                        (String) tuple[1]
                    );
                }

                @Override
                public List transformList(List collection) {
                    return collection;
                }
            } )
            .getResultList();

            PersonAndCountryDTO firstEntry = personAndAddressDTOs.get( 0 );
            Assert.assertEquals( "Chris", firstEntry.getPerson().getName());
            assertEquals("USA", firstEntry.getCountry());
        } );
    }

}
