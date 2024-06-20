package com.vladmihalcea.util;

import com.vladmihalcea.util.providers.DataSourceProvider;
import com.vladmihalcea.util.providers.Database;
import com.vladmihalcea.util.transaction.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.hibernate.*;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.usertype.UserType;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public abstract class AbstractTest {

    public static final boolean ENABLE_LONG_RUNNING_TESTS = false;

    static {
        Thread.currentThread().setName("Alice");
    }

    protected final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread bob = new Thread(r);
        bob.setName("Bob");
        return bob;
    });

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private DataSource dataSource;

    private EntityManagerFactory emf;

    private SessionFactory sf;

    private List<Closeable> closeables = new ArrayList<>();

    @Before
    public void init() {
        beforeInit();
        if (nativeHibernateSessionFactoryBootstrap()) {
            sf = newSessionFactory();
        } else {
            emf = newEntityManagerFactory();
        }
        afterInit();
    }

    protected void beforeInit() {

    }

    protected void afterInit() {

    }

    @After
    public void destroy() {
        if (nativeHibernateSessionFactoryBootstrap()) {
            if (sf != null) {
                sf.close();
            }
        } else {
            if (emf != null) {
                emf.close();
            }
        }
        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (IOException e) {
                LOGGER.error("Failure", e);
            }
        }
        closeables.clear();
        afterDestroy();
    }

    protected void afterDestroy() {

    }

    public EntityManagerFactory entityManagerFactory() {
        return nativeHibernateSessionFactoryBootstrap() ? sf : emf;
    }

    public SessionFactory sessionFactory() {
        if (nativeHibernateSessionFactoryBootstrap()) {
            return sf;
        }
        EntityManagerFactory entityManagerFactory = entityManagerFactory();
        if (entityManagerFactory == null) {
            return null;
        }
        return entityManagerFactory.unwrap(SessionFactory.class);
    }

    protected boolean nativeHibernateSessionFactoryBootstrap() {
        return false;
    }

    protected Class<?>[] entities() {
        return new Class[]{};
    }

    protected List<String> entityClassNames() {
        return Arrays.asList(entities()).stream().map(Class::getName).collect(Collectors.toList());
    }

    protected String[] packages() {
        return null;
    }

    protected String[] resources() {
        return null;
    }

    protected Interceptor interceptor() {
        return null;
    }

    private SessionFactory newSessionFactory() {
        final BootstrapServiceRegistryBuilder bsrb = new BootstrapServiceRegistryBuilder()
            .enableAutoClose();

        Integrator integrator = integrator();
        if (integrator != null) {
            bsrb.applyIntegrator(integrator);
        }

        final BootstrapServiceRegistry bsr = bsrb.build();

        final StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder(bsr)
            .applySettings(properties())
            .build();

        final MetadataSources metadataSources = new MetadataSources(serviceRegistry);

        for (Class annotatedClass : entities()) {
            metadataSources.addAnnotatedClass(annotatedClass);
        }

        String[] packages = packages();
        if (packages != null) {
            for (String annotatedPackage : packages) {
                metadataSources.addPackage(annotatedPackage);
            }
        }

        String[] resources = resources();
        if (resources != null) {
            for (String resource : resources) {
                metadataSources.addResource(resource);
            }
        }

        final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder()
            .applyImplicitNamingStrategy(ImplicitNamingStrategyLegacyJpaImpl.INSTANCE);

        final List<UserType<?>> additionalTypes = additionalTypes();
        if (additionalTypes != null) {
            additionalTypes.forEach(type -> {
                metadataBuilder.applyTypes((typeContributions, sr) -> typeContributions.contributeType(type));
            });
        }

        additionalMetadata(metadataBuilder);

        MetadataImplementor metadata = (MetadataImplementor) metadataBuilder.build();

        final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
        Interceptor interceptor = interceptor();
        if (interceptor != null) {
            sfb.applyInterceptor(interceptor);
        }

        return sfb.build();
    }

    private SessionFactory newLegacySessionFactory() {
        Properties properties = properties();
        Configuration configuration = new Configuration().addProperties(properties);
        for (Class<?> entityClass : entities()) {
            configuration.addAnnotatedClass(entityClass);
        }
        String[] packages = packages();
        if (packages != null) {
            for (String scannedPackage : packages) {
                configuration.addPackage(scannedPackage);
            }
        }
        String[] resources = resources();
        if (resources != null) {
            for (String resource : resources) {
                configuration.addResource(resource);
            }
        }
        Interceptor interceptor = interceptor();
        if (interceptor != null) {
            configuration.setInterceptor(interceptor);
        }

        final List<UserType<?>> additionalTypes = additionalTypes();
        if (additionalTypes != null) {
            configuration.registerTypeContributor(
                (typeContributions, serviceRegistry) ->
                    additionalTypes.forEach(typeContributions::contributeType)
            );
        }
        return configuration.buildSessionFactory(
            new StandardServiceRegistryBuilder()
                .applySettings(properties)
                .build()
        );
    }

    protected EntityManagerFactory newEntityManagerFactory() {
        PersistenceUnitInfo persistenceUnitInfo = persistenceUnitInfo(getClass().getSimpleName());
        Map configuration = properties();
        Interceptor interceptor = interceptor();
        if (interceptor != null) {
            configuration.put(AvailableSettings.INTERCEPTOR, interceptor);
        }
        Integrator integrator = integrator();
        if (integrator != null) {
            configuration.put("hibernate.integrator_provider", (IntegratorProvider) () -> Collections.singletonList(integrator));
        }

        List<UserType<?>> additionalTypes = additionalTypes();
        if (additionalTypes != null) {
            configuration.put("hibernate.type_contributors",
                (TypeContributorList) () -> Collections.singletonList(
                    (typeContributions, serviceRegistry) -> {
                        additionalTypes.forEach(typeContributions::contributeType);
                    }
                ));
        }

        EntityManagerFactoryBuilderImpl entityManagerFactoryBuilder = new EntityManagerFactoryBuilderImpl(
            new PersistenceUnitInfoDescriptor(persistenceUnitInfo), configuration
        );
        return entityManagerFactoryBuilder.build();
    }

    protected Integrator integrator() {
        return null;
    }

    protected PersistenceUnitInfoImpl persistenceUnitInfo(String name) {
        PersistenceUnitInfoImpl persistenceUnitInfo = new PersistenceUnitInfoImpl(
            name, entityClassNames(), properties()
        );
        String[] resources = resources();
        if (resources != null) {
            persistenceUnitInfo.getMappingFileNames().addAll(Arrays.asList(resources));
        }
        return persistenceUnitInfo;
    }

    protected Properties properties() {
        Properties properties = new Properties();
        //log settings
        properties.put("hibernate.hbm2ddl.auto", "none");
        //data source settings
        DataSource dataSource = dataSource();
        if (dataSource != null) {
            properties.put("hibernate.connection.datasource", dataSource);
        }
        additionalProperties(properties);
        return properties;
    }

    protected Dialect dialect() {
        SessionFactory sessionFactory = sessionFactory();
        return sessionFactory != null ?
            sessionFactory.unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect() :
            ReflectionUtils.newInstance(dataSourceProvider().hibernateDialect());
    }

    protected Map<String, Object> propertiesMap() {
        return (Map) properties();
    }

    protected void additionalProperties(Properties properties) {

    }

    protected DataSourceProxyType dataSourceProxyType() {
        return DataSourceProxyType.DATA_SOURCE_PROXY;
    }

    protected DataSource dataSource() {
        if (dataSource == null) {
            dataSource = newDataSource();
        }
        return dataSource;
    }

    protected DataSource newDataSource() {
        DataSource dataSource =
            proxyDataSource()
                ? dataSourceProxyType().dataSource(dataSourceProvider().dataSource())
                : dataSourceProvider().dataSource();
        if (connectionPooling()) {
            HikariDataSource poolingDataSource = connectionPoolDataSource(dataSource);
            closeables.add(poolingDataSource::close);
            return poolingDataSource;
        } else {
            return dataSource;
        }
    }

    protected boolean proxyDataSource() {
        return true;
    }

    protected HikariDataSource connectionPoolDataSource(DataSource dataSource) {
        return new HikariDataSource(hikariConfig(dataSource));
    }

    protected HikariConfig hikariConfig(DataSource dataSource) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setMaximumPoolSize(connectionPoolSize());
        hikariConfig.setDataSource(dataSource);
        return hikariConfig;
    }

    protected boolean connectionPooling() {
        return false;
    }

    protected int connectionPoolSize() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        return cpuCores * 4;
    }

    protected DataSourceProvider dataSourceProvider() {
        return database().dataSourceProvider();
    }

    protected Database database() {
        return Database.POSTGRESQL;
    }

    protected List<UserType<?>> additionalTypes() {
        return null;
    }

    protected void additionalMetadata(MetadataBuilder metadataBuilder) {

    }

    protected <T> T doInJPA(JPATransactionFunction<T> function) {
        T result = null;
        EntityManager entityManager = null;
        EntityTransaction txn = null;
        try {
            entityManager = entityManagerFactory().createEntityManager();
            function.beforeTransactionCompletion();
            txn = entityManager.getTransaction();
            txn.begin();
            result = function.apply(entityManager);
            if (!txn.getRollbackOnly()) {
                txn.commit();
            } else {
                try {
                    txn.rollback();
                } catch (Exception e) {
                    LOGGER.error("Rollback failure", e);
                }
            }
        } catch (Throwable t) {
            if (txn != null && txn.isActive()) {
                try {
                    txn.rollback();
                } catch (Exception e) {
                    LOGGER.error("Rollback failure", e);
                }
            }
            throw t;
        } finally {
            function.afterTransactionCompletion();
            if (entityManager != null) {
                entityManager.close();
            }
        }
        return result;
    }

    protected void doInJPA(JPATransactionVoidFunction function) {
        EntityManager entityManager = null;
        EntityTransaction txn = null;
        try {
            entityManager = entityManagerFactory().createEntityManager();
            function.beforeTransactionCompletion();
            txn = entityManager.getTransaction();
            txn.begin();
            function.accept(entityManager);
            if (!txn.getRollbackOnly()) {
                txn.commit();
            } else {
                try {
                    txn.rollback();
                } catch (Exception e) {
                    LOGGER.error("Rollback failure", e);
                }
            }
        } catch (Throwable t) {
            if (txn != null && txn.isActive()) {
                try {
                    txn.rollback();
                } catch (Exception e) {
                    LOGGER.error("Rollback failure", e);
                }
            }
            throw t;
        } finally {
            function.afterTransactionCompletion();
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    protected void doInJDBC(ConnectionVoidCallable callable) {
        Session session = null;
        Transaction txn = null;
        try {
            session = sessionFactory().openSession();
            session.setDefaultReadOnly(true);
            session.setHibernateFlushMode(FlushMode.MANUAL);
            txn = session.beginTransaction();
            session.doWork(callable::execute);
            if (!txn.getRollbackOnly()) {
                txn.commit();
            } else {
                try {
                    txn.rollback();
                } catch (Exception e) {
                    LOGGER.error("Rollback failure", e);
                }
            }
        } catch (Throwable t) {
            if (txn != null && txn.isActive()) {
                try {
                    txn.rollback();
                } catch (Exception e) {
                    LOGGER.error("Rollback failure", e);
                }
            }
            throw t;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    protected Future<?> executeAsync(Runnable callable) {
        return executorService.submit(callable);
    }

    protected void awaitOnLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected <V> V sleep(int millis, Callable<V> callable) {
        V result = null;
        try {
            if (callable != null) {
                result = callable.call();
            }
            Thread.sleep(millis);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    protected void awaitTermination(long timeout, TimeUnit unit) {
        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void executeStatement(String sql) {
        try (Connection connection = dataSource().getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            LOGGER.error("Statement failed", e);
        }
    }

    protected void executeStatement(String... sqls) {
        try (Connection connection = dataSource().getConnection();
             Statement statement = connection.createStatement()) {
            for(String sql : sqls) {
                statement.executeUpdate(sql);
            }
        } catch (SQLException e) {
            LOGGER.error("Statement failed", e);
        }
    }

    protected void executeStatement(Connection connection, String sql) {
        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void executeStatement(Connection connection, String sql, int timeout) {
        try {
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(timeout);
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void executeStatement(Connection connection, String... sqls) {
        try {
            try (Statement statement = connection.createStatement()) {
                for (String sql : sqls) {
                    statement.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void executeStatement(EntityManager entityManager, String... sqls) {
        Session session = entityManager.unwrap(Session.class);
        for (String sql : sqls) {
            try {
                session.doWork(connection -> {
                    executeStatement(connection, sql);
                });
            } catch (Exception e) {
                LOGGER.error(
                    String.format("Error executing statement: %s", sql), e
                );
            }
        }
    }

    protected int update(Connection connection, String sql, Object[] params) {
        try {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setQueryTimeout(1);
                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }
                return statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected int count(Connection connection, String sql) {
        try {
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(1);
                ResultSet resultSet = statement.executeQuery(sql);
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("There was no row to be selected!");
                }
                return ((Number) resultSet.getObject(1)).intValue();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Set JDBC Connection or Statement timeout
     *
     * @param connection JDBC Connection time out
     */
    public void setJdbcTimeout(Connection connection) {
        setJdbcTimeout(connection, 1000);
    }

    /**
     * Set JDBC Connection or Statement timeout
     *
     * @param connection   JDBC Connection time out
     * @param timoutMillis millis to wait
     */
    public void setJdbcTimeout(Connection connection, long timoutMillis) {
        try (Statement st = connection.createStatement()) {
            DataSourceProvider dataSourceProvider = dataSourceProvider();

            switch (dataSourceProvider.database()) {
                case POSTGRESQL:
                    st.execute(String.format("SET statement_timeout TO %d", timoutMillis));
                    break;
                case MYSQL:
                    st.execute(String.format("SET SESSION innodb_lock_wait_timeout = %d", TimeUnit.MILLISECONDS.toSeconds(timoutMillis)));
                    connection.setNetworkTimeout(Executors.newSingleThreadExecutor(), (int) timoutMillis);
                    break;
                case SQLSERVER:
                    st.execute(String.format("SET LOCK_TIMEOUT %d", timoutMillis));
                    connection.setNetworkTimeout(Executors.newSingleThreadExecutor(), (int) timoutMillis);
                    break;
                default:
                    try {
                        connection.setNetworkTimeout(Executors.newSingleThreadExecutor(), (int) timoutMillis);
                    } catch (Throwable ignore) {
                        ignore.fillInStackTrace();
                    }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
