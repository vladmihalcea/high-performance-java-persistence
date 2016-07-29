alter table post_comment drop constraint FKna4y825fdc5hw8aow65ijexm0;
alter table post_details drop constraint FKkl5eik513p1xiudk2kxb0v92u;
alter table post_tag drop constraint FKac1wdchd2pnur3fl225obmlg0;
alter table post_tag drop constraint FKc2auetuvsec0k566l0eyvr9cs;
drop table post;
drop table post_comment;
drop table post_details;
drop table post_tag;
drop table tag;

create table post (id bigint not null, title varchar(255), primary key (id));
create table post_comment (id bigint not null, review varchar(255), post_id bigint, primary key (id));
create table post_details (id bigint not null, created_by varchar(255), created_on datetime2, updated_by varchar(255), updated_on datetime2, primary key (id));
create table post_tag (post_id bigint not null, tag_id bigint not null);
create table tag (id bigint not null, name varchar(255), primary key (id));
alter table post_comment add constraint FKna4y825fdc5hw8aow65ijexm0 foreign key (post_id) references post;
alter table post_details add constraint FKkl5eik513p1xiudk2kxb0v92u foreign key (id) references post;
alter table post_tag add constraint FKac1wdchd2pnur3fl225obmlg0 foreign key (tag_id) references tag;
alter table post_tag add constraint FKc2auetuvsec0k566l0eyvr9cs foreign key (post_id) references post;
