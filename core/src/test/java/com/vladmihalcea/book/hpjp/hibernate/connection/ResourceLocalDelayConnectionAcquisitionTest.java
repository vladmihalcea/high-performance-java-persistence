package com.vladmihalcea.book.hpjp.hibernate.connection;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.vladmihalcea.flexypool.adaptor.DataSourcePoolAdapter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import oracle.xml.parser.v2.XMLElement;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ResourceLocalDelayConnectionAcquisitionTest extends AbstractTest {

    public static final String DATA_FILE_PATH = "data/weather.xml";

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd" );

    private long warmUpDuration = TimeUnit.SECONDS.toNanos( 10 );

    private long measurementsDuration = TimeUnit.SECONDS.toNanos( 17 );

    private int parseCount = 100;

    private FlexyPoolDataSource flexyPoolDataSource;

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer( getClass().getSimpleName() );

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Forecast.class
        };
    }

    protected boolean connectionPooling() {
        return false;
    }

    protected HikariConfig hikariConfig(DataSource dataSource) {
        HikariConfig hikariConfig = super.hikariConfig( dataSource );
        hikariConfig.setAutoCommit( false );
        return hikariConfig;
    }

    protected HikariDataSource connectionPoolDataSource(DataSource dataSource) {
        HikariConfig hikariConfig = new HikariConfig();
        int cpuCores = Runtime.getRuntime().availableProcessors();
        hikariConfig.setMaximumPoolSize(cpuCores * 4);
        hikariConfig.setDataSource(dataSource);

        return new HikariDataSource(hikariConfig);
    }

    @Override
    protected DataSource newDataSource() {
        DataSource dataSource = super.newDataSource();

        com.vladmihalcea.flexypool.config.Configuration<DataSource> configuration = new com.vladmihalcea.flexypool.config.Configuration.Builder<>(
                getClass().getSimpleName(), dataSource, DataSourcePoolAdapter.FACTORY)
                .setMetricLogReporterMillis( TimeUnit.SECONDS.toMillis( 15 ) )
        .build();
        flexyPoolDataSource = new FlexyPoolDataSource<>( configuration);
        return flexyPoolDataSource;
    }

    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.connection.provider_disables_autocommit", "true");
        return properties;
    }

    @Test
    public void testConnection() {
        long warmUpThreshold = System.nanoTime() + warmUpDuration;
        LOGGER.info( "Warming up" );

        while ( System.nanoTime() < warmUpThreshold ) {
            importForecasts();
        }

        long measurementsThreshold = System.nanoTime() + measurementsDuration;

        LOGGER.info( "Measuring" );
        flexyPoolDataSource.start();
        int transactionCount = 0;
        while ( System.nanoTime() < measurementsThreshold ) {
            importForecasts();
            transactionCount++;
        }
        flexyPoolDataSource.stop();
        logReporter.report();
        LOGGER.info( "Transaction throughput: {}", transactionCount);
    }

    private void importForecasts() {
        doInJPA(entityManager -> {
            long startNanos = System.nanoTime();
            List<Forecast> forecasts = null;
            for ( int i = 0; i < parseCount; i++ ) {
                Document forecastXmlDocument = readXmlDocument( DATA_FILE_PATH );
                forecasts = parseForecasts(forecastXmlDocument);
            }
            timer.update( System.nanoTime() - startNanos, TimeUnit.NANOSECONDS );

            if ( forecasts != null ) {
                for(Forecast forecast : forecasts.subList( 0, 50 )) {
					entityManager.persist( forecast );
				}
            }
        });
    }

    private Document readXmlDocument(String filePath) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream( filePath )) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse( inputStream );

            doc.getDocumentElement().normalize();

            return doc;
        }
        catch (Exception e) {
            throw new IllegalArgumentException( e );
        }
    }

    private List<Forecast> parseForecasts(Document xmlDocument) {
        NodeList cityNodes = xmlDocument.getElementsByTagName( "localitate");
        List<Forecast> forecasts = new ArrayList<>(  );
        for ( int i = 0; i < cityNodes.getLength(); i++ ) {
            Node cityNode = cityNodes.item(i);
            String city = cityNode.getAttributes().getNamedItem( "nume" ).getNodeValue();

            NodeList forecastNodes = ( (XMLElement) cityNode ).getChildrenByTagName( "prognoza" );
            for ( int j = 0; j < forecastNodes.getLength(); j++ ) {
                Node forecastNode = forecastNodes.item(j);

                Forecast forecast = new Forecast();
                forecast.setCity( city );

                String dateValue = forecastNode.getAttributes().getNamedItem( "data" ).getNodeValue();
                try {
                    forecast.setDate( simpleDateFormat.parse( dateValue ) );
                }
                catch (ParseException e) {
                    throw new IllegalArgumentException( e );
                }

                NodeList forecastDetailsNodes = forecastNode.getChildNodes();
                for ( int k = 0; k < forecastDetailsNodes.getLength(); k++ ) {
                    Node forecastDetailsNode = forecastDetailsNodes.item(k);
                    switch ( forecastDetailsNode.getNodeName() ) {
                        case "temp_min":
                            forecast.setTemperatureMin( Byte.valueOf( forecastDetailsNode.getTextContent() ) );
                            break;
                        case "temp_max":
                            forecast.setTemperatureMax( Byte.valueOf( forecastDetailsNode.getTextContent() ) );
                            break;
                        case "fenomen_descriere":
                            forecast.setDescription( forecastDetailsNode.getTextContent() );
                            break;
                    }
                }

                forecasts.add( forecast );
            }
        }
        return forecasts;
    }

    @Entity(name = "Forecast")
    public static class Forecast {

        @Id
        @GeneratedValue
        private Long id;

        private String city;

        @Temporal( TemporalType.DATE )
        @Column(name = "forecast_date")
        private Date date;

        @Column(name = "temperature_min")
        private byte temperatureMin;

        @Column(name = "temperature_max")
        private byte temperatureMax;

        private String description;

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public byte getTemperatureMin() {
            return temperatureMin;
        }

        public void setTemperatureMin(byte temperatureMin) {
            this.temperatureMin = temperatureMin;
        }

        public byte getTemperatureMax() {
            return temperatureMax;
        }

        public void setTemperatureMax(byte temperatureMax) {
            this.temperatureMax = temperatureMax;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
