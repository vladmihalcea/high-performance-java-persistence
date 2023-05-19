drop table if exists post_comment_details;
drop table if exists post_comment;
drop table if exists post_details;
drop table if exists post_tag;
drop table if exists post;
drop table if exists tag;
drop table if exists answer;
drop table if exists question;

drop sequence if exists hibernate_sequence;

create table post (id int8 not null, title varchar(255), primary key (id));
create table post_comment (id int8 not null, review varchar(255), post_id int8, primary key (id));
create table post_details (id int8 not null, created_by varchar(255), created_on timestamp, updated_by varchar(255), updated_on timestamp, primary key (id));
create table post_tag (post_id int8 not null, tag_id int8 not null);
create table tag (id int8 not null, name varchar(255), primary key (id));
create table post_comment_details (id int8 not null, post_id int8 not null, user_id int8 not null, ip varchar(18) not null, fingerprint varchar(256), primary key (id));

create table question (id bigint not null, body varchar(255), created_on timestamp(6) default now(), score integer not null default 0, title varchar(255), updated_on timestamp(6) default now(), primary key (id));
create table answer (id bigint not null, accepted boolean not null default false, body varchar(255), created_on timestamp(6) default now(), score integer not null default 0, updated_on timestamp(6) default now(), question_id bigint, primary key (id));

alter table post_comment add constraint post_comment_post_id foreign key (post_id) references post;
alter table post_details add constraint post_details_post_id foreign key (id) references post;
alter table post_tag add constraint post_tag_tag_id foreign key (tag_id) references tag;
alter table post_tag add constraint post_tag_post_id foreign key (post_id) references post;

alter table if exists answer add constraint answer_question_id foreign key (question_id) references question;

create sequence hibernate_sequence start with 1 increment by 1;

drop function if exists get_updated_questions_and_answers;

CREATE OR REPLACE FUNCTION get_updated_questions_and_answers(updated_after timestamp)
RETURNS TABLE(
    question_id bigint, question_title varchar(255), question_body varchar(255), question_score integer, question_created_on timestamp, question_updated_on timestamp,
    answer_id bigint, answer_body varchar(255), answer_accepted boolean, answer_score integer, answer_created_on timestamp, answer_updated_on timestamp
) AS
$$
SELECT
    question.id, question.title, question.body, question.score, question.created_on, question.updated_on,
    answer.id bigint, answer.body, answer.accepted, answer.score, answer.created_on, answer.updated_on
FROM question
         JOIN answer on question.id = answer.question_id
WHERE
        question.updated_on >= updated_after OR
        answer.updated_on >= updated_after;
$$
LANGUAGE sql
;