drop table if exists schema_version cascade;

alter table post_comment drop constraint POST_COMMENT_POST_ID_FK;
alter table post_details drop constraint POST_DETAILS_POST_ID_FK;
alter table post_tag drop constraint POST_TAG_TAG_ID_FK;
alter table post_tag drop constraint POST_TAG_POST_ID_FK;

drop table post if exists;
drop table post_comment if exists;
drop table post_details if exists;
drop table post_tag if exists;
drop table tag if exists;
drop table users if exists;

drop sequence hibernate_sequence;