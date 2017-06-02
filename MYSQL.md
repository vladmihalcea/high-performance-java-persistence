* Install MySQL 5.6 (or later)
* Create the database `high_performance_java_persistence`
  ```
  create database high_performance_java_persistence;
  ```
* Create the user `mysql` and grant it the necessary privileges:
  ```
  create user 'mysql'@'localhost';
  SET PASSWORD for 'mysql'@'localhost' = PASSWORD('admin');
  GRANT ALL PRIVILEGES ON high_performance_java_persistence.* TO 'mysql'@'localhost';
  GRANT SELECT ON mysql.* TO 'mysql'@'localhost';
  FLUSH PRIVILEGES;
  ```
