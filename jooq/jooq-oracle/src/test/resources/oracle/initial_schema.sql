BEGIN
    EXECUTE IMMEDIATE 'drop table USER_VOTE cascade constraints';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'drop table "USER" cascade constraints';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'drop table POST_COMMENT_details cascade constraints';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'drop table POST_COMMENT cascade constraints';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'drop table POST_DETAILS cascade constraints';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'drop table POST_TAG cascade constraints';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'drop table POST cascade constraints';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'drop table TAG cascade constraints';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
create table POST (ID number(19,0) not null, title varchar2(255 char), primary key (ID))
/
create table POST_COMMENT (ID number(19,0) not null, review varchar2(255 char), POST_ID number(19,0), primary key (ID))
/
create table POST_DETAILS (ID number(19,0) not null, created_by varchar2(255 char), created_on timestamp, updated_by varchar2(255 char), updated_on timestamp, primary key (ID))
/
create table POST_TAG (POST_ID number(19,0) not null, TAG_ID number(19,0) not null)
/
create table TAG (ID number(19,0) not null, name varchar2(255 char), primary key (ID))
/
create table POST_COMMENT_details (ID number(19,0) not null, POST_ID number(19,0) not null, USER_ID number(19,0) not null, ip varchar2(18) not null, fingerprint varchar2(256), primary key (ID))
/
create table "USER" (ID number(19,0) not null, first_name varchar2(50), last_name varchar2(50), primary key (ID))
/
create table USER_VOTE (ID number(19,0) not null, USER_ID number(19,0), COMMENT_ID number(19,0), vote_type number(3,0) not null, primary key (ID))
/
alter table POST_COMMENT add constraint POST_COMMENT_POST_ID foreign key (POST_ID) references POST
/
alter table POST_DETAILS add constraint POST_DETAILS_POST_ID foreign key (ID) references POST
/
alter table POST_TAG add constraint POST_TAG_TAG_ID foreign key (TAG_ID) references TAG
/
alter table POST_TAG add constraint POST_TAG_POST_ID foreign key (POST_ID) references POST
/
alter table USER_VOTE add constraint USER_VOTE_POST_COMMENT_ID foreign key (COMMENT_ID) references POST_COMMENT
/
alter table USER_VOTE add constraint USER_VOTE_USER_ID foreign key (USER_ID) references "USER"
/
drop sequence HIBERNATE_SEQUENCE
/
create sequence HIBERNATE_SEQUENCE start with 1 increment by 1
/