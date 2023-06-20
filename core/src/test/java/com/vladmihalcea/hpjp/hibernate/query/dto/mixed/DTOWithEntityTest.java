package com.vladmihalcea.hpjp.hibernate.query.dto.mixed;

import com.vladmihalcea.hpjp.util.AbstractTest;
import io.hypersistence.utils.hibernate.query.ListResultTransformer;
import org.hibernate.query.Query;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class DTOWithEntityTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Person.class,
            Country.class
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Country usa = new Country();
            usa.setName("USA");
            usa.setLocale("en-US");
            entityManager.persist(usa);

            Country romania = new Country();
            romania.setName("Romania");
            romania.setLocale("ro-RO");
            entityManager.persist(romania);

            Person chris = new Person();
            chris.setName("Chris");
            chris.setLocale("en-US");
            entityManager.persist(chris);

            Person vlad = new Person();
            vlad.setName("Vlad");
            vlad.setLocale("ro-RO");
            entityManager.persist(vlad);
        });

        doInJPA(entityManager -> {
            LOGGER.info( "Using constructor resultl set" );
            List<PersonAndCountryDTO> personAndAddressDTOs = entityManager.createQuery("""
                select new
                   com.vladmihalcea.hpjp.hibernate.query.dto.mixed.PersonAndCountryDTO(
                       p,
                       c.name
                   )
                from Person p
                join Country c on p.locale = c.locale
                order by p.id
                """, PersonAndCountryDTO.class)
            .getResultList();

            PersonAndCountryDTO firstEntry = personAndAddressDTOs.get(0);
            assertEquals("Chris", firstEntry.getPerson().getName());
            assertEquals("USA", firstEntry.getCountry());
        });

        doInJPA(entityManager -> {
            LOGGER.info( "Using ResultTransformer" );
            List<PersonAndCountryDTO> personAndAddressDTOs = entityManager.createQuery("""
                select p, c.name
                from Person p
                join Country c on p.locale = c.locale
                order by p.id
                """)
            .unwrap( Query.class )
            .setResultTransformer(
                (ListResultTransformer) (tuple, aliases) -> new PersonAndCountryDTO(
                    (Person) tuple[0],
                    (String) tuple[1]
                )
            )
            .getResultList();

            PersonAndCountryDTO firstEntry = personAndAddressDTOs.get(0);
            assertEquals("Chris", firstEntry.getPerson().getName());
            assertEquals("USA", firstEntry.getCountry());
        });
    }
}
