package com.vladmihalcea.hpjp.hibernate.query.spatial;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class SpatialTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Address.class,
        };
    }

    @Override
    protected void beforeInit() {
        executeStatement("CREATE EXTENSION IF NOT EXISTS \"postgis\"");
    }

    @Override
    public void afterDestroy() {
        executeStatement("DROP EXTENSION \"postgis\" CASCADE");
    }

    @Test
    public void test() {
        Long addressId = doInJPA(entityManager -> {
            try {
                Address address = new Address();
                address.setId(1L);
                address.setStreet("5th Avenue");
                address.setNumber("1 A");
                address.setLocation((Point) new WKTReader().read("POINT(60 12)"));

                entityManager.persist(address);
                return address.getId();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        });

        doInJPA(entityManager -> {
            Address address = entityManager.find(Address.class, addressId);
            Coordinate coordinate = address.getLocation().getCoordinate();
            assertEquals(60.0d, coordinate.getOrdinate(Coordinate.X), 0.1);
            assertEquals(12.0d, coordinate.getOrdinate(Coordinate.Y), 0.1);
        });
    }

    @Entity(name = "Address")
    public static class Address  {

        @Id
        private Long id;

        private String street;

        private String number;

        private Point location;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public Point getLocation() {
            return location;
        }

        public void setLocation(Point location) {
            this.location = location;
        }
    }
}
