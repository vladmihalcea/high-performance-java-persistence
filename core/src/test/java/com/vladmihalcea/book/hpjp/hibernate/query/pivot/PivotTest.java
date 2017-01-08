package com.vladmihalcea.book.hpjp.hibernate.query.pivot;

import com.vladmihalcea.book.hpjp.util.AbstractOracleXEIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.query.Query;
import org.hibernate.transform.Transformers;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PivotTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Service.class,
            Component.class,
            Property.class
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Service apollo = new Service();
            apollo.setName("Apollo");
            entityManager.persist(apollo);

            Service artemis = new Service();
            artemis.setName("Artemis");
            entityManager.persist(artemis);
        });

        doInJPA(entityManager -> {
            Component dataSource = new Component();
            dataSource.setName("dataSource");
            entityManager.persist(dataSource);

            Component flexyPoolDataSource = new Component();
            flexyPoolDataSource.setName("flexyPoolDataSource");
            entityManager.persist(flexyPoolDataSource);
        });

        doInJPA(entityManager -> {
            Service apollo = entityManager.find(Service.class, "Apollo");

            Component dataSource = entityManager.find(Component.class, "dataSource");

            Property databaseName = new Property();
            databaseName.setId(
                    new PropertyId(apollo, dataSource, "databaseName")
            );
            databaseName.setValue("high_performance_java_persistence");
            entityManager.persist(databaseName);

            Property serverName = new Property();
            serverName.setId(
                    new PropertyId(apollo, dataSource, "serverName")
            );
            serverName.setValue("192.168.0.5");
            entityManager.persist(serverName);

            Property user = new Property();
            user.setId(
                    new PropertyId(apollo, dataSource, "username")
            );
            user.setValue("postgres");
            entityManager.persist(user);

            Property password = new Property();
            password.setId(
                    new PropertyId(apollo, dataSource, "password")
            );
            password.setValue("admin");
            entityManager.persist(password);

            Component flexyPoolDataSource = entityManager.find(Component.class, "flexyPoolDataSource");

            Property uniqueName = new Property();
            uniqueName.setId(
                    new PropertyId(apollo, flexyPoolDataSource, "flexy.pool.data.source.unique.name")
            );
            uniqueName.setValue("apolloDS");
            entityManager.persist(uniqueName);

            Property jmxAutoStart = new Property();
            jmxAutoStart.setId(
                    new PropertyId(apollo, flexyPoolDataSource, "flexy.pool.metrics.reporter.jmx.auto.start")
            );
            jmxAutoStart.setValue("true");
            entityManager.persist(jmxAutoStart);
        });

        doInJPA(entityManager -> {
            Service artemis = entityManager.find(Service.class, "Artemis");

            Component dataSource = entityManager.find(Component.class, "dataSource");

            Property databaseName = new Property();
            databaseName.setId(
                    new PropertyId(artemis, dataSource, "databaseName")
            );
            databaseName.setValue("high_performance_java_persistence");
            entityManager.persist(databaseName);

            Property serverName = new Property();
            serverName.setId(
                    new PropertyId(artemis, dataSource, "url")
            );
            serverName.setValue("jdbc:oracle:thin:@192.169.0.6:1521/hpjp");
            entityManager.persist(serverName);

            Property user = new Property();
            user.setId(
                    new PropertyId(artemis, dataSource, "username")
            );
            user.setValue("oracle");
            entityManager.persist(user);

            Property password = new Property();
            password.setId(
                    new PropertyId(artemis, dataSource, "password")
            );
            password.setValue("admin");
            entityManager.persist(password);

            Component flexyPoolDataSource = entityManager.find(Component.class, "flexyPoolDataSource");

            Property uniqueName = new Property();
            uniqueName.setId(
                    new PropertyId(artemis, flexyPoolDataSource, "flexy.pool.data.source.unique.name")
            );
            uniqueName.setValue("artemisDS");
            entityManager.persist(uniqueName);

            Property reporterLogMillis = new Property();
            reporterLogMillis.setId(
                    new PropertyId(artemis, flexyPoolDataSource, "flexy.pool.metrics.reporter.log.millis")
            );
            reporterLogMillis.setValue(String.valueOf(TimeUnit.MINUTES.toMillis(1)));
            entityManager.persist(reporterLogMillis);
        });

        doInJPA(entityManager -> {
           List<Property> componentProperties = entityManager.createQuery(
                "SELECT p " +
                "FROM Property p " +
                "WHERE p.id.component.name = :name", Property.class)
            .setParameter("name", "flexyPoolDataSource")
            .getResultList();
            assertEquals(4, componentProperties.size());
        });

        doInJPA(entityManager -> {
            List<Object[]> componentProperties = entityManager.createNativeQuery(
                "SELECT " +
                "   p.service_name AS serviceName, " +
                "   p.component_name AS componentName, " +
                "   p.property_name, " +
                "   p.property_value " +
                "FROM Property p " +
                "WHERE " +
                "   p.component_name = :name")
            .setParameter("name", "dataSource")
            .getResultList();
            assertEquals(8, componentProperties.size());
        });

        doInJPA(entityManager -> {
            List<DataSourceConfiguration> dataSources = entityManager
            .createNativeQuery(
                "SELECT distinct " +
                "   userName.service_name AS \"serviceName\", " +
                "   c.name AS \"componentName\", " +
                "   databaseName.property_value AS \"databaseName\", " +
                "   url.property_value AS \"url\", " +
                "   serverName.property_value AS \"serverName\", " +
                "   userName.property_value AS \"userName\", " +
                "   password.property_value AS \"password\" " +
                "FROM Component c " +
                "left join Property databaseName " +
                "   on databaseName.component_name = c.name and " +
                "      databaseName.property_name = 'databaseName' " +
                "left join Property url " +
                "   on url.component_name = c.name and " +
                "      url.property_name = 'url' " +
                "left join Property serverName " +
                "   on serverName.component_name = c.name and " +
                "      serverName.property_name = 'serverName' " +
                "left join Property userName " +
                "   on userName.component_name = c.name and " +
                "      userName.property_name = 'username' " +
                "left join Property password " +
                "   on password.component_name = c.name and " +
                "      password.property_name = 'password' " +
                "WHERE " +
                "   c.name = :name")
            .setParameter("name", "dataSource")
            .unwrap(Query.class)
            .setResultTransformer(Transformers.aliasToBean(DataSourceConfiguration.class))
            .getResultList();
            assertEquals(2, dataSources.size());
        });

        doInJPA(entityManager -> {
            List<DataSourceConfiguration> dataSources = entityManager.createNativeQuery(
                "SELECT " +
                "   p.service_name AS \"serviceName\", " +
                "   p.component_name AS \"componentName\", " +
                "   MAX(CASE WHEN property_name = 'databaseName' " +
                "       THEN property_value END) AS \"databaseName\", " +
                "   MAX(CASE WHEN property_name = 'url' " +
                "       THEN property_value END) AS \"url\", " +
                "   MAX(CASE WHEN property_name = 'serverName' " +
                "       THEN property_value END) AS \"serverName\", " +
                "   MAX(CASE WHEN property_name = 'username' " +
                "       THEN property_value END) AS \"userName\", " +
                "   MAX(CASE WHEN property_name = 'password' " +
                "   THEN property_value END) AS \"password\" " +
                "FROM Property p " +
                "WHERE " +
                "   p.component_name = :name " +
                "GROUP BY p.service_name, p.component_name")
            .setParameter("name", "dataSource")
            .unwrap(Query.class)
            .setResultTransformer(Transformers.aliasToBean(DataSourceConfiguration.class))
            .getResultList();
            assertEquals(2, dataSources.size());
        });

        //http://modern-sql.com/use-case/pivot

        doInJPA(entityManager -> {
            List<DataSourceConfiguration> dataSources = entityManager
            .createNativeQuery(
                "SELECT * FROM ( " +
                "   SELECT " +
                "       p.service_name AS \"serviceName\", " +
                "       p.component_name AS \"componentName\", " +
                "       p.property_name , " +
                "       p.property_value " +
                "   FROM Property p " +
                "   WHERE " +
                "       p.component_name = :name" +
                ") " +
                "PIVOT(" +
                "   MAX(property_value) " +
                "   FOR property_name IN (" +
                "       'databaseName' AS \"databaseName\", " +
                "       'url' AS \"url\", " +
                "       'serverName' AS \"serverName\", " +
                "       'username' AS \"userName\", " +
                "       'password' AS \"password\") " +
                ")")
            .setParameter("name", "dataSource")
            .unwrap(Query.class)
            .setResultTransformer(
                Transformers.aliasToBean(
                    DataSourceConfiguration.class
                )
            )
            .getResultList();
            assertEquals(2, dataSources.size());
        });
    }
}
