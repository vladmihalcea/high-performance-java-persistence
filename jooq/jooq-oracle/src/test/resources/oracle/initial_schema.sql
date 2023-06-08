BEGIN
EXECUTE IMMEDIATE 'drop table user_vote cascade constraints';
EXCEPTION WHEN OTHERS THEN NULL;
END;

BEGIN
EXECUTE IMMEDIATE 'drop table blog_user cascade constraints';
EXCEPTION WHEN OTHERS THEN NULL;
END;

BEGIN
EXECUTE IMMEDIATE 'drop table post_comment_details cascade constraints';
EXCEPTION WHEN OTHERS THEN NULL;
END;

BEGIN
EXECUTE IMMEDIATE 'drop table post_comment cascade constraints';
EXCEPTION WHEN OTHERS THEN NULL;
END;

BEGIN
EXECUTE IMMEDIATE 'drop table post_details cascade constraints';
EXCEPTION WHEN OTHERS THEN NULL;
END;

BEGIN
EXECUTE IMMEDIATE 'drop table post_tag cascade constraints';
EXCEPTION WHEN OTHERS THEN NULL;
END;

BEGIN
EXECUTE IMMEDIATE 'drop table post cascade constraints';
EXCEPTION WHEN OTHERS THEN NULL;
END;

BEGIN
EXECUTE IMMEDIATE 'drop table tag cascade constraints';
EXCEPTION WHEN OTHERS THEN NULL;
END;

create table post (id number(19,0) not null, title varchar2(255 char), primary key (id));
create table post_comment (id number(19,0) not null, review varchar2(255 char), post_id number(19,0), primary key (id));
create table post_details (id number(19,0) not null, created_by varchar2(255 char), created_on timestamp, updated_by varchar2(255 char), updated_on timestamp, primary key (id));
create table post_tag (post_id number(19,0) not null, tag_id number(19,0) not null);
create table tag (id number(19,0) not null, name varchar2(255 char), primary key (id));
create table post_comment_details (id number(19,0) not null, post_id number(19,0) not null, user_id number(19,0) not null, ip varchar2(18) not null, fingerprint varchar2(256), primary key (id));
create table blog_user (id number(19,0) not null, first_name varchar2(50), last_name varchar2(50), primary key (id));
create table user_vote (id number(19,0) not null, user_id number(19,0), comment_id number(19,0), score integer not null, primary key (id));

alter table post_comment add constraint post_comment_post_id foreign key (post_id) references post;
alter table post_details add constraint post_details_post_id foreign key (id) references post;
alter table post_tag add constraint post_tag_tag_id foreign key (tag_id) references tag;
alter table post_tag add constraint post_tag_post_id foreign key (post_id) references post;

drop sequence hibernate_sequence;

create sequence hibernate_sequence start with 1 increment by 1;