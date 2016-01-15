drop table if exists schema_version cascade;

alter table post_comment drop constraint POST_COMMENT_POST_ID_FK;
alter table post_details drop constraint POST_DETAILS_POST_ID_FK;
alter table post_tag drop constraint POST_TAG_TAG_ID_FK;
alter table post_tag drop constraint POST_TAG_POST_ID_FK;

drop table if exists post cascade;
drop table if exists post_comment cascade;
drop table if exists post_details cascade;
drop table if exists post_tag cascade;
drop table if exists tag cascade;
drop table if exists users cascade;

drop sequence hibernate_sequence;