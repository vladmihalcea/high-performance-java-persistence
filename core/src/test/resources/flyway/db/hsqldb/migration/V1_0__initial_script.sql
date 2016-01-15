create sequence hibernate_sequence start with 1 increment by 1;

create table post (id bigint not null, title varchar(255), primary key (id));
create table tag (id bigint not null, name varchar(255), primary key (id));