create table post_tag (post_id bigint not null, tag_id bigint not null);

alter table post_tag add constraint POST_TAG_TAG_ID_FK foreign key (tag_id) references tag (id);
alter table post_tag add constraint POST_TAG_POST_ID_FK foreign key (post_id) references post (id);