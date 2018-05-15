/**
 * checkout:
 * <p>
 * - http://hsqldb.org/
 * - http://www.h2database.com/html/main.html
 * </p>
 */


package com.sorinc.test.hibernate.one2many;

import com.sorinc.test.hibernate.cfg.JpaHibernateH2ConfigurationTest;
import com.sorinc.test.hibernate.cfg.RepositoryConfigurationTest;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {RepositoryConfigurationTest.class, JpaHibernateH2ConfigurationTest.class})
public abstract class JpaHibernateTest extends AbstractProviderTest {

    private static final Logger logger = LoggerFactory.getLogger(JpaHibernateTest.class);



    @Before
    public void before() {
//
//        SessionFactory sessionFactory = new Configuration().configure()
//                .buildSessionFactory();
//
//        Session session = sessionFactory.openSession();
//        session.doWork(new Work() {
//            @Override
//            public void execute(Connection connection) throws SQLException {
//                try {
//                    File script = new File(getClass().getResource("/db/data.sql").getFile());
//                    RunScript.execute(connection, new FileReader(script));
//                } catch (FileNotFoundException e) {
//                    throw new RuntimeException("could not initialize with script");
//                }
//            }
//        });
    }

    @After
    public void after() {

    }


//    @Bean
//    public PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() throws IOException {
//        final PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
//        ppc.setLocations(ArrayUtils.addAll(
//                new PathMatchingResourcePatternResolver().getResources("classpath*:application.properties"),
//                new PathMatchingResourcePatternResolver().getResources("classpath*:test.properties")
//                )
//        );
//
//        return ppc;
//    }
}
