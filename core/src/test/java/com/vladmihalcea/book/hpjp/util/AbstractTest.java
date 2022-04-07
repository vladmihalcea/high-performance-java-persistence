package com.vladmihalcea.book.hpjp.util;

import com.vladmihalcea.book.hpjp.util.exception.DataAccessException;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.book.hpjp.util.providers.LockType;
import com.vladmihalcea.book.hpjp.util.transaction.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
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
import org.hibernate.cache.internal.QueryResultsCacheImpl;
import org.hibernate.cache.jcache.internal.JCacheAccessImpl;
import org.hibernate.cache.spi.entry.CollectionCacheEntry;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.cache.spi.support.*;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.usertype.UserType;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

public abstract class AbstractTest {

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
        if(nativeHibernateSessionFactoryBootstrap()) {
            sf = newSessionFactory();
        } else {
            emf = newEntityManagerFactory();
        }
        afterInit();
    }

    protected void afterInit() {

    }

    @After
    public void destroy() {
        if(nativeHibernateSessionFactoryBootstrap()) {
            if (sf != null) {
                sf.close();
            }
        } else {
            if (emf != null) {
                emf.close();
            }
        }
        for(Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (IOException e) {
                LOGGER.error("Failure", e);
            }
        }
        closeables.clear();
    }

    public EntityManagerFactory entityManagerFactory() {
        return nativeHibernateSessionFactoryBootstrap() ? sf : emf;
    }

    public SessionFactory sessionFactory() {
        return nativeHibernateSessionFactoryBootstrap() ? sf : entityManagerFactory().unwrap(SessionFactory.class);
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
            bsrb.applyIntegrator( integrator );
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
        if(interceptor != null) {
            sfb.applyInterceptor(interceptor);
        }

        return sfb.build();
    }

    private SessionFactory newLegacySessionFactory() {
        Properties properties = properties();
        Configuration configuration = new Configuration().addProperties(properties);
        for(Class<?> entityClass : entities()) {
            configuration.addAnnotatedClass(entityClass);
        }
        String[] packages = packages();
        if(packages != null) {
            for(String scannedPackage : packages) {
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
        if(interceptor != null) {
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
        properties.put("hibernate.dialect", dataSourceProvider().hibernateDialect());
        //log settings
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        //data source settings
        DataSource dataSource = dataSource();
        if (dataSource != null) {
            properties.put("hibernate.connection.datasource", dataSource);
        }
        properties.put("hibernate.generate_statistics", Boolean.TRUE.toString());

        properties.put("net.sf.ehcache.configurationResourceName", Thread.currentThread().getContextClassLoader().getResource("ehcache.xml").toString());
        //properties.put("hibernate.ejb.metamodel.population", "disabled");
        additionalProperties(properties);
        return properties;
    }

    protected Dialect dialect() {
        return sessionFactory().unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect();
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
        if(dataSource == null) {
            dataSource = newDataSource();
        }
        return dataSource;
    }

    protected DataSource newDataSource() {
        DataSource dataSource =
        proxyDataSource()
            ? dataSourceProxyType().dataSource(dataSourceProvider().dataSource())
            : dataSourceProvider().dataSource();
        if(connectionPooling()) {
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
        int cpuCores = Runtime.getRuntime().availableProcessors();
        hikariConfig.setMaximumPoolSize(cpuCores * 4);
        hikariConfig.setDataSource(dataSource);
        return hikariConfig;
    }

    protected boolean connectionPooling() {
        return false;
    }

    protected DataSourceProvider dataSourceProvider() {
        return database().dataSourceProvider();
    }

    protected Database database() {
        return Database.HSQLDB;
    }

    protected List<UserType<?>> additionalTypes() {
        return null;
    }

    protected void additionalMetadata(MetadataBuilder metadataBuilder) {

    }

    protected <T> T doInHibernate(HibernateTransactionFunction<T> callable) {
        T result = null;
        Session session = null;
        Transaction txn = null;
        try {
            session = sessionFactory().openSession();
            callable.beforeTransactionCompletion();
            txn = session.beginTransaction();

            result = callable.apply(session);
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
            callable.afterTransactionCompletion();
            if (session != null) {
                session.close();
            }
        }
        return result;
    }

    protected void doInHibernate(HibernateTransactionConsumer callable) {
        Session session = null;
        Transaction txn = null;
        try {
            session = sessionFactory().openSession();
            callable.beforeTransactionCompletion();
            txn = session.beginTransaction();

            callable.accept(session);
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
            callable.afterTransactionCompletion();
            if (session != null) {
                session.close();
            }
        }
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
            function.afterTransactionCompletion();
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    protected <T> T doInJDBC(ConnectionCallable<T> callable) {
        AtomicReference<T> result = new AtomicReference<>();
        Session session = null;
        Transaction txn = null;
        try {
            session = sessionFactory().openSession();
            txn = session.beginTransaction();
            session.doWork(connection -> {
                result.set(callable.execute(connection));
            });
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
            if (session != null) {
                session.close();
            }
        }
        return result.get();
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
            if (session != null) {
                session.close();
            }
        }
    }

    protected void executeSync(VoidCallable callable) {
        executeSync(Collections.singleton(callable));
    }

    protected <T> T executeSync(Callable<T> callable) {
        try {
            return executorService.submit(callable).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    protected void executeSync(Collection<VoidCallable> callables) {
        try {
            List<Future<Void>> futures = executorService.invokeAll(callables);
            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    protected void executeAsync(Runnable callable, final Runnable completionCallback) {
        final Future future = executorService.submit(callable);
        new Thread(() -> {
            while (!future.isDone()) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            try {
                completionCallback.run();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).start();
    }

    protected Future<?> executeAsync(Runnable callable) {
        return executorService.submit(callable);
    }

    protected  void transact(Consumer<Connection> callback) {
        transact(callback, null);
    }

    protected  void transact(Consumer<Connection> callback, Consumer<Connection> before) {
        Connection connection = null;
        try {
            connection = dataSource().getConnection();
            if (before != null) {
                before.accept(connection);
            }
            connection.setAutoCommit(false);
            callback.accept(connection);
            connection.commit();
        } catch (Exception e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    throw new DataAccessException( e);
                }
            }
            throw (e instanceof DataAccessException ?
                    (DataAccessException) e : new DataAccessException(e));
        } finally {
            if(connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    throw new DataAccessException(e);
                }
            }
        }
    }

    protected LockType lockType() {
        return LockType.LOCKS;
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

    protected String selectStringColumn(Connection connection, String sql) {
        try {
            try(Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(1);
                ResultSet resultSet = statement.executeQuery(sql);
                if(!resultSet.next()) {
                    throw new IllegalArgumentException("There was no row to be selected!");
                }
                return resultSet.getString(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected <T> T selectColumn(Connection connection, String sql, Class<T> clazz) {
        return selectColumn(connection, sql, clazz, null);
    }

    protected <T> T selectColumn(Connection connection, String sql, Class<T> clazz, Duration timeout) {
        try {
            try(Statement statement = connection.createStatement()) {
                if (timeout != null) {
                    statement.setQueryTimeout((int) timeout.toSeconds());
                }
                ResultSet resultSet = statement.executeQuery(sql);
                if(!resultSet.next()) {
                    throw new IllegalArgumentException("There was no row to be selected!");
                }
                return clazz.cast(resultSet.getObject(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected <T> List<T> selectColumnList(Connection connection, String sql, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        try {
            try(Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(1);
                ResultSet resultSet = statement.executeQuery(sql);
                while (resultSet.next()) {
                    result.add(clazz.cast(resultSet.getObject(1)));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    protected int update(Connection connection, String sql) {
        try {
            try(Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(1);
                return statement.executeUpdate(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
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

    protected void executeStatement(Connection connection, String sql) {
        try {
            try(Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void executeStatement(Connection connection, String sql, int timeout) {
        try {
            try(Statement statement = connection.createStatement()) {
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
            try(PreparedStatement statement = connection.prepareStatement(sql)) {
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
            try(Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(1);
                ResultSet resultSet = statement.executeQuery(sql);
                if(!resultSet.next()) {
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
     * @param connection JDBC Connection time out
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
            fail(e.getMessage());
        }
    }

    protected String transactionId(EntityManager entityManager) {
        return String.valueOf(
            entityManager.createNativeQuery(
                dataSourceProvider()
                .queries()
                .transactionId()
            )
            .getSingleResult()
        );
    }

    protected void printEntityCacheRegionStatistics(Class<?> entityClass) {
        printCacheRegionStatisticsEntries(entityClass.getName());
    }

    protected void printCollectionCacheRegionStatistics(Class<?> entityClass, String collection) {
        printCacheRegionStatisticsEntries(entityClass.getName() + "." + collection);
    }

    protected void printQueryCacheRegionStatistics() {
        printCacheRegionStatisticsEntries("default-query-results-region");
    }

    protected void printNaturalIdCacheRegionStatistics(Class<?> entityClass) {
        printCacheRegionStatistics(entityClass.getName() + "##NaturalId");
    }

    protected void printCacheRegionStatistics(String region) {
        printCacheRegionStatisticsEntries(region);
    }

    private void printCacheRegionStatisticsEntries(String regionName) {
        SessionFactory sessionFactory = sessionFactory();
        Statistics statistics = sessionFactory.getStatistics();
        if (sessionFactory.getSessionFactoryOptions().isQueryCacheEnabled()) {
            ReflectionUtils.invokeMethod(statistics, "getQueryRegionStats", "default-query-results-region");
        }

        CacheRegionStatistics cacheRegionStatistics = "default-query-results-region".equals(regionName) ?
            statistics.getQueryRegionStatistics(regionName) :
            statistics.getDomainDataRegionStatistics(regionName);

        if (cacheRegionStatistics != null) {
            AbstractRegion region = ReflectionUtils.getFieldValue(cacheRegionStatistics, "region");

            StorageAccess storageAccess = getStorageAccess(region);
            org.ehcache.core.Ehcache cache = getEhcache(storageAccess);

            if (cache != null) {
                StringBuilder cacheEntriesBuilder = new StringBuilder();
                cacheEntriesBuilder.append("[");

                boolean firstEntry = true;

                //TODO: Get keys
                for (Object key : cache.getAll(Collections.emptySet()).entrySet()) {
                    Object cacheValue = storageAccess.getFromCache(key, null);

                    if (!firstEntry) {
                        cacheEntriesBuilder.append(",\n");
                    } else {
                        cacheEntriesBuilder.append("\n");
                        firstEntry = false;
                    }
                    cacheEntriesBuilder.append("\t");

                    if (cacheValue instanceof QueryResultsCacheImpl.CacheItem) {
                        QueryResultsCacheImpl.CacheItem queryValue = (QueryResultsCacheImpl.CacheItem) cacheValue;

                        cacheEntriesBuilder.append(
                            ToStringBuilder.reflectionToString(queryValue, ToStringStyle.SHORT_PREFIX_STYLE)
                        );
                    } else if (cacheValue instanceof StandardCacheEntryImpl) {
                        StandardCacheEntryImpl standardCacheEntry = (StandardCacheEntryImpl) cacheValue;

                        cacheEntriesBuilder.append(
                            ToStringBuilder.reflectionToString(standardCacheEntry, ToStringStyle.SHORT_PREFIX_STYLE)
                        );
                    } else if (cacheValue instanceof CollectionCacheEntry) {
                        CollectionCacheEntry collectionCacheEntry = (CollectionCacheEntry) cacheValue;

                        cacheEntriesBuilder.append(
                            ToStringBuilder.reflectionToString(collectionCacheEntry, ToStringStyle.SHORT_PREFIX_STYLE)
                        );
                    } else if (cacheValue instanceof AbstractReadWriteAccess.Item) {
                        AbstractReadWriteAccess.Item valueItem = (AbstractReadWriteAccess.Item) cacheValue;
                        Object value = valueItem.getValue();

                        if (value instanceof StandardCacheEntryImpl) {
                            StandardCacheEntryImpl standardCacheEntry = ((StandardCacheEntryImpl) value);
                            cacheEntriesBuilder.append(
                                ToStringBuilder.reflectionToString(standardCacheEntry, ToStringStyle.SHORT_PREFIX_STYLE)
                            );
                        } else if(value.getClass().getPackageName().startsWith("java")) {
                            cacheEntriesBuilder.append(value);
                        } else {
                            cacheEntriesBuilder.append(
                                ToStringBuilder.reflectionToString(valueItem.getValue(), ToStringStyle.SHORT_PREFIX_STYLE)
                            );
                        }
                    } else if (cacheValue instanceof AbstractReadWriteAccess.Lockable) {
                        cacheEntriesBuilder.append(
                            ToStringBuilder.reflectionToString(cacheValue, ToStringStyle.SHORT_PREFIX_STYLE)
                        );
                    }
                }

                cacheEntriesBuilder.append("\n]");

                LOGGER.debug(
                    "\nRegion: {},\nStatistics: {},\nEntries: {}",
                    regionName,
                    cacheRegionStatistics,
                    cacheEntriesBuilder
                );
            }
        }
    }

    private org.ehcache.core.Ehcache getEhcache(StorageAccess storageAccess) {
        if(storageAccess instanceof JCacheAccessImpl) {
            return null;
        }
        return ReflectionUtils.getFieldValue(storageAccess, "cache");
    }


    private StorageAccess getStorageAccess(AbstractRegion region) {
        if(region instanceof DirectAccessRegionTemplate) {
            DirectAccessRegionTemplate directAccessRegionTemplate = (DirectAccessRegionTemplate) region;
            return directAccessRegionTemplate.getStorageAccess();
        }
        else if(region instanceof DomainDataRegionTemplate) {
            DomainDataRegionTemplate domainDataRegionTemplate = (DomainDataRegionTemplate) region;
            return domainDataRegionTemplate.getCacheStorageAccess();
        }
        throw new IllegalArgumentException("Unsupported region: " + region);
    }

    public static String stringValue(Object value) {
        return value.toString();
    }

    public static int intValue(Object number) {
        return ((Number) number).intValue();
    }

    public static long longValue(Object number) {
        if(number instanceof String) {
            return Long.parseLong((String) number);
        }
        return ((Number) number).longValue();
    }

    public static double doubleValue(Object number) {
        return ((Number) number).doubleValue();
    }

    public static URL urlValue(String url) {
        try {
            return url != null ? new URL(url) : null;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected List<Map<String, String>> parseResultSet(ResultSet resultSet) {
        List<Map<String, String>> rows = new ArrayList<>();

        try {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), resultSet.getString(i));
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }

        return rows;
    }
}
