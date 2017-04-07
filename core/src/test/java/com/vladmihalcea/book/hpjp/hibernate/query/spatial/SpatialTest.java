package com.vladmihalcea.book.hpjp.hibernate.query.spatial;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;

import org.hibernate.annotations.Type;
import org.hibernate.spatial.dialect.postgis.PostgisDialect;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;

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
    protected DataSourceProvider dataSourceProvider() {
        return new PostgreSQLDataSourceProvider() {
            @Override
            public String hibernateDialect() {
                return PostgisDialect.class.getName();
            }
        };
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

        @Type(type = "jts_geometry")
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
