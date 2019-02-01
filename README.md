# High-Performance Java Persistence

The [High-Performance Java Persistence](https://vladmihalcea.com/books/high-performance-java-persistence?utm_source=GitHub&utm_medium=banner&utm_campaign=hpjp) book and video course code examples. I wrote [this article](https://vladmihalcea.com/high-performance-java-persistence-github-repository/) about this repository since it's one of the best way to test JDBC, JPA, Hibernate or even jOOQ code. Or, if you prefer videos, you can watch [this presentation on YouTube](https://www.youtube.com/watch?v=U8MoOe8uMYA).

### Are you struggling with application performance issues?

I could help you get the most out of your data access layer.

If you are interested in on-site training, I can offer you my [High-Performance Java Persistence training](https://vladmihalcea.com/trainings/?utm_source=GitHub&utm_medium=banner&utm_campaign=hibernatetypes)
which can be adapted to one, two or three days of sessions. For more details, check out [my website](https://vladmihalcea.com/trainings/?utm_source=GitHub&utm_medium=banner&utm_campaign=hibernatetypes).

If you want me to review your application and provide insight into how you can optimize it to run faster, 
then check out my [consulting page](https://vladmihalcea.com/consulting/?utm_source=GitHub&utm_medium=banner&utm_campaign=hibernatetypes).

If you want the fastest way to learn how to speed up a Java database application, then you should definitely enroll in [my High-Performance Java Persistence video courses](https://vladmihalcea.com/courses/?utm_source=GitHub&utm_medium=banner&utm_campaign=hibernatetypes).

Or, if you prefer reading books, you are going to love my [High-Performance Java Persistence book](https://vladmihalcea.com/books/high-performance-java-persistence?utm_source=GitHub&utm_medium=banner&utm_campaign=hibernatetypes) as well.

<a href="https://vladmihalcea.com/books/high-performance-java-persistence?utm_source=GitHub&utm_medium=banner&utm_campaign=hibernatetypes">
<img src="https://i0.wp.com/vladmihalcea.com/wp-content/uploads/2018/01/HPJP_h200.jpg" alt="High-Performance Java Persistence book">
</a>

<a href="https://vladmihalcea.com/courses?utm_source=GitHub&utm_medium=banner&utm_campaign=hibernatetypes">
<img src="https://i0.wp.com/vladmihalcea.com/wp-content/uploads/2018/01/HPJP_Video_Vertical_h200.jpg" alt="High-Performance Java Persistence video course">
</a>

## Java

All examples require at least Java 1.8. 

**Javac compiler is required in order to run in in any IDE environment**.
 
Especially if you're using Eclipse, you must use the Oracle JDK compiler and not the Eclipse-based one which suffers from [this issue](https://bugs.eclipse.org/bugs/show_bug.cgi?id=434642).

## IntelliJ IDEA

On IntelliJ IDEA, the project runs just fine without any further requirements.

## Eclipse

However, on Eclipse it has been reported that you need to consider the following configurations (many thanks to [Urs Joss](https://github.com/ursjoss) for the hints):

1. Eclipse does not automatically treat the generated sources by jpamodelgen as source folders. You need to add a dependency on `hibernate-jpamodelgen` and use the `build-helper-maven-plugin` to source the folders with the generated sources.
2. Secondly, the Maven eclipse plugin e2m seems to have an issue with some plugin configurations. Make sure you configure e2m to ignore the false positives issues (the project runs justs fine from a Maven command line).
3. There’s an issue with Eclipse (or probably more specific ecj) to infer the types of parameters in case of method overloading with the methods `doInJpa`, `doInHibernate`, `doInJdbc`. 
Until [this Eclipse issue](https://bugs.eclipse.org/bugs/show_bug.cgi?id=434642) is fixed, you need to use the Oracle JDK to compile the project.
If you can't change that, you need to rename those overloaded functions as explained by Urs Joss in [this specific commit](https://github.com/ursjoss/high-performance-java-persistence/commit/e975c1bb5c11d9557fcbc3fef88afaf67dc68a25).

## Database setup

The Unit Tests are run against HSQLDB, so no preliminary set-ups are required.

The Integration Tests require some external configurations:

- PostgreSQL

    You should install PostgreSQL 9.5 (or later) and the password for the postgres user should be admin

    Now you need to create a `high_performance_java_persistence` database
    Open pgAdmin III and executed the following query:
    
        CREATE EXTENSION postgis;
        CREATE EXTENSION pgcrypto;
        CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    
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

    2. You can also download the Oracle JDBC Driver (ojdbc7_g.jar and ojdbc8.jar), which is not available in the Maven Central Repository.
    and install the ojdbc7_g.jar and ojdbc8.jar on your local Maven repository using the following command:

            $ mvn install:install-file -Dfile=ojdbc8.jar -DgroupId=com.oracle.jdbc -DartifactId=ojdbc8 -Dversion=12.2.0.1 -Dpackaging=jar
            $ mvn install:install-file -Dfile=ojdbc7_g.jar -DgroupId=com.oracle -DartifactId=ojdbc7_g -Dversion=12.1.0.1 -Dpackaging=jar            

    The `com.oracle:ojdbc7_g` artifact is used just by the jooq-oracle sub-module since there is some issue with the sql-maven-plugin Oracle dependency otherwise.

- MySQL

    You should install MySQL 5.6 (or later) and the password for the mysql user should be admin.

    Now you need to create a `high_performance_java_persistence` schema

    Besides having all privileges on this schema, the user mysql also requires select persmission on `mysql.PROC`.

    Exact instructions can be found in [here](https://github.com/vladmihalcea/high-performance-java-persistence/blob/master/MYSQL.md).

- SQL Server

    You should install SQL Server Express Edition with Tools Chose mixed mode authentication and set the sa user password to adm1n

    Open SQL Server Configuration Manager -> SQL Server Network Configuration and enable Named Pipes and TCP
    
    In the right pane of the TCP/IP option, choose Properties, then IP Addresses and make sure you Enable all listed IP addresses.
    You need to blank the dynamic TCP port value and configure the static TCP port 1433 for all IPs.
        
    Open SQL Server Management Studio and create the `high_performance_java_persistence` database
    
## Maven

To build the project, don't use *install* or *package*. Instead, just compile test classes like this:

    mvn clean test-compile
    
Then, just pick one test from the IDE and run it individually.
If you run all tests (e.g. `mvn clean test`), the test suite will take way to long to complete since
some performance tests require to run for long periods of time.
