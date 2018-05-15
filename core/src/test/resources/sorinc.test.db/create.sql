


--USE hcs_aaa ;

-- set fk checks OFF
SET foreign_key_checks = 0;

drop table if exists account;
drop table if exists token;
drop table if exists contact;
--
SET foreign_key_checks = 1;

-- drop sequence hibernate_sequence;
-- create sequence hibernate_sequence start with 1 increment by 1;

-- account tbl
create table account(
  id bigint(20) AUTO_INCREMENT primary key,
  first_name varchar(200) not null,
  last_name varchar(200) not null,
  mid_name varchar(100) ,
  username varchar(200) not null,
  password varchar(200) not null, -- PASSWORD HAVE TO BE STORED ENCRYPTED !!!
  email varchar(200) not null,
  phone varchar(200) not null,
  type varchar(100) not null,
  country varchar(100) not null,
  county varchar(50),
  town varchar(100),
  street varchar(100),
  zip_code varchar(100),
  created_at timestamp default current_timestamp,
  updated_at timestamp default current_timestamp
 ) ;

-- token tbl
create table token (
  id bigint(20) AUTO_INCREMENT primary key,
  reference varchar(36) not null,
  jwt varchar(300) not null,
  account_id bigint(20) not null,
  created_at timestamp default current_timestamp,
  updated_at timestamp default current_timestamp,

  CONSTRAINT tokens_fk FOREIGN KEY (account_id) REFERENCES account(id)
) ;

-- contact table
CREATE TABLE contact (
  id bigint(20) AUTO_INCREMENT primary key,
  first_name varchar(100) not null,
  last_name varchar(100) not null,
  email varchar(200) not null,
  phone varchar(100) not null,
  postal_code varchar(20) not null,

  created_at timestamp default current_timestamp,
  updated_at timestamp default current_timestamp
) ;