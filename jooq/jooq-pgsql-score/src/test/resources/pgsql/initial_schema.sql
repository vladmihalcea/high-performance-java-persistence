drop table if exists post cascade
;
drop table if exists post_comment cascade
;

drop sequence hibernate_sequence
;

create sequence hibernate_sequence start 1 increment 1
;

create table post (id int8 not null, title varchar(255), primary key (id))
;
create table post_comment (id int8 not null, created_on timestamp, review varchar(255), score int4 not null, parent_id int8, post_id int8, primary key (id))
;

alter table post_comment add constraint FKmqxhu8q0j94rcly3yxlv0u498 foreign key (parent_id) references post_comment
;
alter table post_comment add constraint FKna4y825fdc5hw8aow65ijexm0 foreign key (post_id) references post
;