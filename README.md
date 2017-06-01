# High-Performance Java Persistence
The [High-Performance Java Persistence](https://leanpub.com/high-performance-java-persistence?utm_source=GitHub&utm_medium=banner&utm_campaign=hpjp) book code examples.

<a href="https://leanpub.com/high-performance-java-persistence?utm_source=GitHub&utm_medium=banner&utm_campaign=hpjp">
<img src="https://vladmihalcea.files.wordpress.com/2015/11/hpjp_small.jpg" alt="High-Performance Java Persistence">
</a>

All examples require at least Java 1.8. 

**Javac compiler is required in order to run in in any IDE environment. 
Especially if you're using Eclipse, you must use the Oracle JDK compiler and not the Eclipse-based one which suffers from [this issue](https://bugs.eclipse.org/bugs/show_bug.cgi?id=434642).**

On InteliJ IDEA, the project runs just fine without any further requirements.

However, on Eclipse it has been reported that you need to consider the following configurations (many thanks to [Urs Joss](https://github.com/ursjoss) for the hints):

1. Eclipse does not automatically treat the generated sources by jpamodelgen as source folders. You need to add a dependency on `hibernate-jpamodelgen` and use the `build-helper-maven-plugin` to source the folders with the generated sources.
2. Secondly, the Maven eclipse plugin e2m seems to have an issue with some plugin configurations. Make sure you configure e2m to ignore the false positives issues (the project runs justs fine from a Maven command line).
3. There’s an issue with Eclipse (or probably more specific ecj) to infer the types of parameters in case of method overloading with the methods `doInJPA`, `doInHibernate`, `doInJDBC`.
Until [this Eclipse issue](https://bugs.eclipse.org/bugs/show_bug.cgi?id=434642) is fixed, you need to use the Oracle JDK to compile the project.
If you can't change that, you need to rename those overloaded functions as explained by Urs Joss in [this specific commit](https://github.com/ursjoss/high-performance-java-persistence/commit/88373dc7c2080d02e2e3965afae961495e7d956b).

Urs applied all those changes in his branch [tb_eclipse](https://github.com/ursjoss/high-performance-java-persistence/tree/tb_eclipse) and tries to keep abreast with the evolution of this repository.

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
    You need to blank the dynamic TCP port value and configure the static TCP port 1433 for all IPs.
        
    Open SQL Server Management Studio and create the `high_performance_java_persistence` database
