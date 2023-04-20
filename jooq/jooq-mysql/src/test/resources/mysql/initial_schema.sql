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

alter table post_comment add constraint post_comment_post_id foreign key (post_id) references post (id);
alter table post_details add constraint post_details_post_id foreign key (id) references post (id);
alter table post_tag add constraint post_tag_tag_id foreign key (tag_id) references tag (id);
alter table post_tag add constraint post_tag_post_id foreign key (post_id) references post (id);