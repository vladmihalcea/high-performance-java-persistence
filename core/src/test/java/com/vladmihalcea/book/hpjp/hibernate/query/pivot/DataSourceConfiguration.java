package com.vladmihalcea.book.hpjp.hibernate.query.pivot;

/**
 * @author Vlad Mihalcea
 */
public class DataSourceConfiguration {

    private String serviceName;
    private String componentName;
    private String databaseName;
    private String url;
    private String serverName;
    private String userName;
    private String password;

    public DataSourceConfiguration() {
    }

    public DataSourceConfiguration(String serviceName, String componentName, String databaseName, String url, String serverName, String userName, String password) {
        this.serviceName = serviceName;
        this.componentName = componentName;
        this.databaseName = databaseName;
        this.url = url;
        this.serverName = serverName;
        this.userName = userName;
        this.password = password;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
