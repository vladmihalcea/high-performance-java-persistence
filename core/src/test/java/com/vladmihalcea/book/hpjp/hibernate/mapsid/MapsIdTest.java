package com.vladmihalcea.book.hpjp.hibernate.mapsid;

import com.vladmihalcea.book.hpjp.hibernate.car.OwnerCar;
import com.vladmihalcea.book.hpjp.hibernate.car.Car;
import com.vladmihalcea.book.hpjp.hibernate.car.Owner;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertNotNull;

public class MapsIdTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Owner.class,
                OwnerCar.class,
                Car.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.show_sql", true);
        properties.put("hibernate.format_sql", true);
        properties.put("javax.persistence.sql-load-script-source", "seed.sql");
        return properties;
    }

    @Test
    public void testMapsId() {
        Car car = doInJPA(entityManager -> {
          return entityManager.find(Car.class, 1L);
        });

        //car
        assertNotNull(car);

        //Owner
        Owner owner = new Owner();
        owner.setName("Andy");

        //OwnerCar
        OwnerCar ownerCar = new OwnerCar(car.getId());
        ownerCar.setCar(car);
        ownerCar.setOwner(owner);

        owner.addOwnerCar(ownerCar);

        doInJPA(entityManager -> {
            entityManager.persist(owner);
        });
    }
}
