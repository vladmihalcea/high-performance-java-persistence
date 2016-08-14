drop table if exists post_comment_details;
drop table if exists post_comment;
drop table if exists post_details;
drop table if exists post_tag;
drop table if exists post;
drop table if exists tag;

create table post (id int8 not null, title varchar(255), primary key (id));
create table post_comment (id int8 not null, review varchar(255), post_id int8, primary key (id));
create table post_details (id int8 not null, created_by varchar(255), created_on timestamp, updated_by varchar(255), updated_on timestamp, primary key (id));
create table post_tag (post_id int8 not null, tag_id int8 not null);
create table tag (id int8 not null, name varchar(255), primary key (id));
create table post_comment_details (id int8 not null, post_id int8 not null, user_id int8 not null, ip varchar(18) not null, fingerprint varchar(256), primary key (id));

alter table post_comment add constraint FKna4y825fdc5hw8aow65ijexm0 foreign key (post_id) references post;
alter table post_details add constraint FKkl5eik513p1xiudk2kxb0v92u foreign key (id) references post;
alter table post_tag add constraint FKac1wdchd2pnur3fl225obmlg0 foreign key (tag_id) references tag;
alter table post_tag add constraint FKc2auetuvsec0k566l0eyvr9cs foreign key (post_id) references post;