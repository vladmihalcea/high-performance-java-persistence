# High-Performance Java Persistence
The [High-Performance Java Persistence](https://leanpub.com/high-performance-java-persistence) book code examples

All examples require at least Java 1.8.

The Unit Tests are run against HSQLDB, so no preliminary set-ups are required.

The Integration Tests require some external configurations:

- PostgreSQL

    You should install PostgreSQL 9.4 (or later) and the password for the postgres user should be admin

    Now you need to create a `high_performance_java_persistence` database

- Oracle

    You need to download and install Oracle XE

    Set the sys password to admin

    Connect to Oracle using the "sys as sysdba" user and create a new user:

        create user oracle identified by admin default tablespace users;

        grant dba to oracle;

        alter system set processes=1000 scope=spfile;

        alter system set sessions=1000 scope=spfile;

    You need to download the Orcale JDBC Driver (ojdbc6.jar and ojdbc7_g.jar), which is not available in the Maven Central Repository.

    You need to install the ojdbc6.jar and ojdbc7_g.jar on your local Maven repository using the following command:

        $ mvn install:install-file -Dfile=ojdbc6.jar -DgroupId=com.oracle -DartifactId=ojdbc6 -Dversion=11.2.0.4 -Dpackaging=jar
        $ mvn install:install-file -Dfile=ojdbc7_g.jar -DgroupId=com.oracle -DartifactId=ojdbc7_g -Dversion=12.1.0.1 -Dpackaging=jar 

- MySQL

    You should install MySQL 5.6 (or later) and the password for the mysql user should be admin

    Now you need to create a `high_performance_java_persistence` schema

- SQL Server

    You should install SQL Server Express Edition with Tools Chose mixed mode authentication and set the sa user password to adm1n

    Open SQL Server Management Studio and create the hibernate_master_class database

    Open SQL Server Configuration Manager -> SQL Server Network Configuration and enable Named Pipes and TCP

    You need to download the SQL Server JDBC Driver and install the sqljdbc4.jar on your local Maven repository using the following command:

        $ mvn install:install-file -Dfile=sqljdbc4.jar -Dpackaging=jar -DgroupId=com.microsoft.sqlserver -DartifactId=sqljdbc4 -Dversion=4.0
        
    Now you need to create a `high_performance_java_persistence` database