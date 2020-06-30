create table post_comment (id bigint not null, review varchar(255), post_id bigint, primary key (id));

alter table post_comment add constraint POST_COMMENT_POST_ID_FK foreign key (post_id) references post (id);