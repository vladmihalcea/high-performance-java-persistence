drop table post_comment_details cascade constraints;
drop table post_comment cascade constraints;
drop table post_details cascade constraints;
drop table post_tag cascade constraints;
drop table post cascade constraints;
drop table tag cascade constraints;

drop sequence hibernate_sequence;

create table post (id number(19,0) not null, title varchar2(255 char), primary key (id));
create table post_comment (id number(19,0) not null, review varchar2(255 char), post_id number(19,0), primary key (id));
create table post_details (id number(19,0) not null, created_by varchar2(255 char), created_on timestamp, updated_by varchar2(255 char), updated_on timestamp, primary key (id));
create table post_tag (post_id number(19,0) not null, tag_id number(19,0) not null);
create table tag (id number(19,0) not null, name varchar2(255 char), primary key (id));
create table post_comment_details (id number(19,0) not null, post_id number(19,0) not null, user_id number(19,0) not null, ip varchar(18) not null, fingerprint varchar(256), primary key (id));

alter table post_comment add constraint post_comment_post_id foreign key (post_id) references post;
alter table post_details add constraint post_details_post_id foreign key (id) references post;
alter table post_tag add constraint post_tag_tag_id foreign key (tag_id) references tag;
alter table post_tag add constraint post_tag_post_id foreign key (post_id) references post;

create sequence hibernate_sequence start with 1 increment by 1;