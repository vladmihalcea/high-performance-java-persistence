create table post_tag (post_id int8 not null, tag_id int8 not null);

alter table post_tag add constraint POST_TAG_TAG_ID_FK foreign key (tag_id) references tag;
alter table post_tag add constraint POST_TAG_POST_ID_FK foreign key (post_id) references post;