package com.vladmihalcea.book.hpjp.hibernate.query.spatial;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.*;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.hibernate.annotations.JavaType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.spatial.JTSGeometryJavaType;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.util.HashMap;
import java.util.Map;

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
    public void init() {
        PersistenceUnitInfo persistenceUnitInfo = persistenceUnitInfo(getClass().getSimpleName());
        persistenceUnitInfo.getProperties().put(AvailableSettings.HBM2DDL_AUTO, "none");
        Map<String, Object> configuration = new HashMap<>();
        EntityManagerFactoryBuilderImpl entityManagerFactoryBuilder = new EntityManagerFactoryBuilderImpl(
            new PersistenceUnitInfoDescriptor(persistenceUnitInfo), configuration
        );
        EntityManagerFactory entityManagerFactory = entityManagerFactoryBuilder.build();

        EntityManager entityManager = null;
        EntityTransaction txn = null;
        try {
            entityManager = entityManagerFactory.createEntityManager();
            txn = entityManager.getTransaction();
            txn.begin();

            entityManager.createNativeQuery(
                    "CREATE EXTENSION IF NOT EXISTS postgis"
            ).executeUpdate();

            if ( !txn.getRollbackOnly() ) {
                txn.commit();
            }
            else {
                try {
                    txn.rollback();
                }
                catch (Exception e) {
                    LOGGER.error( "Rollback failure", e );
                }
            }
        } catch (Throwable t) {
            if ( txn != null && txn.isActive() ) {
                try {
                    txn.rollback();
                }
                catch (Exception e) {
                    LOGGER.error( "Rollback failure", e );
                }
            }
            throw t;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
            entityManagerFactory.close();
        }
        super.init();
    }

    @Override
    public void destroy() {
        doInJPA(entityManager -> {
            entityManager.createNativeQuery(
                    "DROP EXTENSION postgis CASCADE"
            ).executeUpdate();
        });
        super.destroy();
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

        @JavaType(JTSGeometryJavaType.class)
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
