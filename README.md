# High-Performance Java Persistence
The [High-Performance Java Persistence](https://leanpub.com/high-performance-java-persistence?utm_source=GitHub&utm_medium=banner&utm_campaign=hpjp) book code examples.

<a href="https://leanpub.com/high-performance-java-persistence?utm_source=GitHub&utm_medium=banner&utm_campaign=hpjp">
<img src="https://vladmihalcea.files.wordpress.com/2015/11/hpjp_small.jpg" alt="High-Performance Java Persistence">
</a>

All examples require at least Java 1.8. Javac compiler might be required in order to run in IDEA environment.

The Unit Tests are run against HSQLDB, so no preliminary set-ups are required.

The Integration Tests require some external configurations:

- PostgreSQL

    You should install PostgreSQL 9.5 (or later) and the password for the postgres user should be admin

    Now you need to create a `high_performance_java_persistence` database
    Open pgAdmin III and executed the following query:
    
        CREATE EXTENSION postgis;
        CREATE EXTENSION pgcrypto;
    
- Oracle

    You need to download and install Oracle XE

    Set the sys password to admin

    Connect to Oracle using the "sys as sysdba" user and create a new user:

        create user oracle identified by admin default tablespace users;

        grant dba to oracle;

        alter system set processes=1000 scope=spfile;

        alter system set sessions=1000 scope=spfile;
        
        ALTER PROFILE DEFAULT LIMIT PASSWORD_LIFE_TIME UNLIMITED;

    For the Oracle JDBC driver, you have multiple alternatives.
    
    1. You can follow the steps explained in [this article](http://docs.oracle.com/middleware/1213/core/MAVEN/config_maven_repo.htm#MAVEN9010) to set up the Oracle Maven Repository.

    2. You can also download the Oracle JDBC Driver (ojdbc6.jar and ojdbc7_g.jar), which is not available in the Maven Central Repository.
    and install the ojdbc6.jar and ojdbc7_g.jar on your local Maven repository using the following command:

            $ mvn install:install-file -Dfile=ojdbc6.jar -DgroupId=com.oracle.jdbc -DartifactId=ojdbc6 -Dversion=11.2.0.4 -Dpackaging=jar
            $ mvn install:install-file -Dfile=ojdbc7_g.jar -DgroupId=com.oracle.jdbc -DartifactId=ojdbc7 -Dversion=12.1.0.2 -Dpackaging=jar 

- MySQL

    You should install MySQL 5.6 (or later) and the password for the mysql user should be admin

    Now you need to create a `high_performance_java_persistence` schema

- SQL Server

    You should install SQL Server Express Edition with Tools Chose mixed mode authentication and set the sa user password to adm1n

    Open SQL Server Configuration Manager -> SQL Server Network Configuration and enable Named Pipes and TCP
    
    In the right pane of the TCP/IP option, choose Properties, then IP Addresses and make sure you Enable all listed IP addresses.
    You need to blank the dynamic TCP port value, and configure the static TCP port 1433 for all IPs.
        
    Open SQL Server Management Studio and create the `high_performance_java_persistence` database
