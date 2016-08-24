drop table if exists post_comment_details;
drop table if exists post_comment;
drop table if exists post_details;
drop table if exists post_tag;
drop table if exists post;
drop table if exists tag;

create table post (id bigint not null AUTO_INCREMENT, title varchar(255), primary key (id));
create table post_comment (id bigint not null AUTO_INCREMENT, review varchar(255), post_id bigint, primary key (id));
create table post_details (id bigint not null, created_by varchar(255), created_on datetime, updated_by varchar(255), updated_on datetime, primary key (id));
create table post_tag (post_id bigint not null, tag_id bigint not null);
create table tag (id bigint not null AUTO_INCREMENT, name varchar(255), primary key (id));
create table post_comment_details (id int8 not null, post_id int8 not null, user_id int8 not null, ip varchar(18) not null, fingerprint varchar(256), primary key (id));

alter table post_comment add constraint FKna4y825fdc5hw8aow65ijexm0 foreign key (post_id) references post (id);
alter table post_details add constraint FKkl5eik513p1xiudk2kxb0v92u foreign key (id) references post (id);
alter table post_tag add constraint FKac1wdchd2pnur3fl225obmlg0 foreign key (tag_id) references tag (id);
alter table post_tag add constraint FKc2auetuvsec0k566l0eyvr9cs foreign key (post_id) references post (id);