drop table post_comment_details cascade constraints;
drop table post_comment cascade constraints;
drop table post_details cascade constraints;
drop table post_tag cascade constraints;
drop table post cascade constraints;
drop table tag cascade constraints;

create table post (id number(19,0) not null, title varchar2(255 char), primary key (id));
create table post_comment (id number(19,0) not null, review varchar2(255 char), post_id number(19,0), primary key (id));
create table post_details (id number(19,0) not null, created_by varchar2(255 char), created_on timestamp, updated_by varchar2(255 char), updated_on timestamp, primary key (id));
create table post_tag (post_id number(19,0) not null, tag_id number(19,0) not null);
create table tag (id number(19,0) not null, name varchar2(255 char), primary key (id));
create table post_comment_details (id number(19,0) not null, post_id number(19,0) not null, user_id number(19,0) not null, ip varchar(18) not null, fingerprint varchar(256), primary key (id));

alter table post_comment add constraint FKna4y825fdc5hw8aow65ijexm0 foreign key (post_id) references post;
alter table post_details add constraint FKkl5eik513p1xiudk2kxb0v92u foreign key (id) references post;
alter table post_tag add constraint FKac1wdchd2pnur3fl225obmlg0 foreign key (tag_id) references tag;
alter table post_tag add constraint FKc2auetuvsec0k566l0eyvr9cs foreign key (post_id) references post;
