package com.vladmihalcea.book.hpjp.hibernate.identifier;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

public class HiloIdentifierTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                HiloIdentifierTest.Hilo.class
        };
    }

    @Test
    public void testHiloIdentifierGenerator() {
        doInJPA(entityManager -> {
            for(int i = 0; i < 8; i++) {
                Hilo hilo = new Hilo();
                entityManager.persist(hilo);
            }
        });
    }

    /**
     * Hilo - Hilo
     *
     * @author Vlad Mihalcea
     */
    @Entity(name = "hilo")
    public static class Hilo {

        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hilo_sequence_generator")
        @GenericGenerator(
                name = "hilo_sequence_generator",
                strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
                parameters = {
                        @Parameter(name = "sequence_name", value = "hilo_seqeunce"),
                        @Parameter(name = "initial_value", value = "1"),
                        @Parameter(name = "increment_size", value = "3"),
                        @Parameter(name = "optimizer", value = "hilo")
                })
        @Id
        private Long id;

    }


}
