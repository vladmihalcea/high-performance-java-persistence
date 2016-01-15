create sequence hibernate_sequence start 1 increment 1;
create table post (id int8 not null, title varchar(255), primary key (id));
create table tag (id int8 not null, name varchar(255), primary key (id));