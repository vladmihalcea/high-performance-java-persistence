package com.vladmihalcea.book.hpjp.hibernate.query;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class RunningTotalTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Animal.class,
            Stable.class,
            AnimalStable.class,
        };
    }

    @Test
    public void test() {
        final AtomicReference<Stable> stable1Holder = new AtomicReference<>();

        final AtomicReference<Animal> animal1Holder = new AtomicReference<>();
        final AtomicReference<Animal> animal2Holder = new AtomicReference<>();

        doInJPA(entityManager -> {
            Stable stable1 = new Stable();
            stable1.id = 1L;
            stable1.title = "Stable 1";
            entityManager.persist(stable1);

            stable1Holder.set(stable1);

            Stable stable2 = new Stable();
            stable2.id = 2L;
            stable2.title = "Stable 2";
            entityManager.persist(stable2);

            Animal linda = new Animal();
            linda.id = 1L;
            linda.name = "Linda";
            entityManager.persist(linda);

            animal1Holder.set(linda);

            Animal berta = new Animal();
            berta.id = 2L;
            berta.name = "Berta";
            entityManager.persist(berta);

            animal2Holder.set(berta);

            Animal siggi = new Animal();
            siggi.id = 3L;
            siggi.name = "Siggi";
            entityManager.persist(siggi);

            AnimalStable animalStable1 = new AnimalStable();
            animalStable1.id = 1L;
            animalStable1.animal = linda;
            animalStable1.stable = stable1;
            animalStable1.registeredOn = Date.from(LocalDate.of(2017, 1, 10).atStartOfDay().toInstant(ZoneOffset.UTC));
            entityManager.persist(animalStable1);

            AnimalStable animalStable2 = new AnimalStable();
            animalStable2.id = 2L;
            animalStable2.animal = linda;
            animalStable2.stable = stable2;
            animalStable2.registeredOn = Date.from(LocalDate.of(2017, 1, 11).atStartOfDay().toInstant(ZoneOffset.UTC));
            entityManager.persist(animalStable2);

            AnimalStable animalStable3 = new AnimalStable();
            animalStable3.id = 3L;
            animalStable3.animal = berta;
            animalStable3.stable = stable1;
            animalStable3.registeredOn = Date.from(LocalDate.of(2017, 1, 11).atStartOfDay().toInstant(ZoneOffset.UTC));
            entityManager.persist(animalStable3);

            AnimalStable animalStable4 = new AnimalStable();
            animalStable4.id = 4L;
            animalStable4.animal = linda;
            animalStable4.stable = stable1;
            animalStable4.registeredOn = Date.from(LocalDate.of(2017, 1, 12).atStartOfDay().toInstant(ZoneOffset.UTC));
            entityManager.persist(animalStable4);

            AnimalStable animalStable5 = new AnimalStable();
            animalStable5.id = 5L;
            animalStable5.animal = linda;
            animalStable5.stable = stable2;
            animalStable5.registeredOn = Date.from(LocalDate.of(2017, 1, 13).atStartOfDay().toInstant(ZoneOffset.UTC));
            entityManager.persist(animalStable5);

            AnimalStable animalStable6 = new AnimalStable();
            animalStable6.id = 6L;
            animalStable6.animal = siggi;
            animalStable6.stable = stable1;
            animalStable6.registeredOn = Date.from(LocalDate.of(2017, 1, 14).atStartOfDay().toInstant(ZoneOffset.UTC));
            entityManager.persist(animalStable6);

        });

        doInJPA(entityManager -> {

            Stable stable1 = stable1Holder.get();
            Animal linda = animal1Holder.get();
            Animal berta = animal2Holder.get();

            List<Animal> animals = entityManager.createNativeQuery(
                "select a.id, a.name " +
                "from animal_stable a_s1 " +
                "join ( " +
                "   select " +
                "       animal_id, " +
                "       max(registered_on) max_registered_on " +
                "   from animal_stable a_s " +
                "   where a_s.registered_on <= :date " +
                "   group by animal_id  " +
                ") a_s2 " +
                "on a_s1.animal_id = a_s2.animal_id " +
                "   and a_s1.registered_on = a_s2.max_registered_on " +
                "join animal a on a.id = a_s1.animal_id " +
                "where a_s1.stable_id = :stable " +
                "order by a_s1.animal_id", Animal.class)
            .setParameter("stable", stable1.id)
            .setParameter("date",
                Date.from(
                    LocalDate.of(2017, 1, 12).atStartOfDay()
                    .toInstant(ZoneOffset.UTC)),
                TemporalType.DATE)
            .getResultList();

            assertEquals(2, animals.size());
            assertEquals(linda, animals.get(0));
            assertEquals(berta, animals.get(1));
        });

        doInJPA(entityManager -> {

            Stable stable1 = stable1Holder.get();
            Animal linda = animal1Holder.get();
            Animal berta = animal2Holder.get();

            List<Animal> animals = entityManager.createNativeQuery(
                "select distinct a.id, a.name " +
                "from ( " +
                "    select " +
                "    animal_id, " +
                "    last_value(stable_id) over ( " +
                "            partition by a_s.animal_id " +
                "            order by a_s.registered_on " +
                "            range between unbounded preceding and " +
                "            unbounded following " +
                "    ) as last_stable_id " +
                "    from animal_stable a_s " +
                "    where a_s.registered_on <= :date " +
                ") a_s1 " +
                "join animal a on a.id = a_s1.animal_id " +
                "where a_s1.last_stable_id = :stable", Animal.class)
            .setParameter("stable", stable1.id)
            .setParameter("date",
                Date.from(
                    LocalDate.of(2017, 1, 12).atStartOfDay()
                    .toInstant(ZoneOffset.UTC)),
                TemporalType.DATE)
            .getResultList();

            assertEquals(2, animals.size());
            assertEquals(linda, animals.get(0));
            assertEquals(berta, animals.get(1));
        });
    }

    @Entity(name = "Stable")
    public static class Stable  {

        @Id
        private Long id;

        private String title;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Stable)) return false;
            Stable stable = (Stable) o;
            return Objects.equals(title, stable.title);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title);
        }
    }

    @Entity(name = "Animal")
    public static class Animal  {

        @Id
        private Long id;

        private String name;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Animal)) return false;
            Animal animal = (Animal) o;
            return Objects.equals(name, animal.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    @Entity(name = "AnimalStable")
    @Table(name = "animal_stable")
    public static class AnimalStable {

        @Id
        private Long id;

        @Column(name = "registered_on")
        @Temporal(TemporalType.DATE)
        private Date registeredOn;

        @ManyToOne(fetch = FetchType.LAZY)
        private Animal animal;

        @ManyToOne(fetch = FetchType.LAZY)
        private Stable stable;
    }

}
