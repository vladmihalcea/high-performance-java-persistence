drop table if exists post_comment_details;
drop table if exists post_comment;
drop table if exists post_details;
drop table if exists post_tag;
drop table if exists post;
drop table if exists tag;
drop table if exists answer;
drop table if exists question;

drop sequence if exists hibernate_sequence;

create table post (id int8 not null, title varchar(250), primary key (id));
create table post_comment (id int8 not null, review varchar(250), post_id int8, primary key (id));
create table post_details (id int8 not null, created_by varchar(250), created_on timestamp, updated_by varchar(250), updated_on timestamp, primary key (id));
create table post_tag (post_id int8 not null, tag_id int8 not null);
create table tag (id int8 not null, name varchar(50), primary key (id));
create table post_comment_details (id int8 not null, post_id int8 not null, user_id int8 not null, ip varchar(18) not null, fingerprint varchar(256), primary key (id));

create table question (id bigint not null, body text, created_on timestamp default now(), score integer not null default 0, title varchar(250), updated_on timestamp default now(), primary key (id));
create table answer (id bigint not null, accepted boolean not null default false, body text, created_on timestamp default now(), score integer not null default 0, updated_on timestamp default now(), question_id bigint, primary key (id));

alter table post_comment add constraint post_comment_post_id foreign key (post_id) references post;
alter table post_details add constraint post_details_post_id foreign key (id) references post;
alter table post_tag add constraint post_tag_tag_id foreign key (tag_id) references tag;
alter table post_tag add constraint post_tag_post_id foreign key (post_id) references post;

alter table if exists answer add constraint answer_question_id foreign key (question_id) references question;

create sequence hibernate_sequence start with 1 increment by 1;

drop function if exists get_updated_questions_and_answers;

CREATE OR REPLACE FUNCTION get_updated_questions_and_answers(from_timestamp timestamp)
RETURNS TABLE(
    question_id bigint, question_title varchar(250), question_body text, question_score integer, question_created_on timestamp, question_updated_on timestamp,
    answer_id bigint, answer_body text, answer_accepted boolean, answer_score integer, answer_created_on timestamp, answer_updated_on timestamp
) AS
$$
    SELECT
        q1.id, q1.title, q1.body, q1.score, q1.created_on, q1.updated_on,
        a1.id bigint, a1.body, a1.accepted, a1.score, a1.created_on, a1.updated_on
    FROM question q1
    LEFT JOIN answer a1 on q1.id = a1.question_id
    WHERE
        q1.id IN (
            SELECT q2.id
            FROM question q2
            WHERE
                q2.updated_on > from_timestamp
        ) OR
        q1.id IN (
            SELECT a2.question_id
            FROM answer a2
            WHERE
                a2.updated_on > from_timestamp
        );
$$
LANGUAGE sql
;

drop function if exists set_updated_on_timestamp;

CREATE FUNCTION set_updated_on_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_on = now();
    RETURN NEW;
END
$$
language 'plpgsql'
;

drop trigger if exists question_set_updated_on_trigger on question;
drop trigger if exists answer_set_updated_on_trigger on answer;

CREATE TRIGGER question_set_updated_on_trigger
BEFORE UPDATE OR DELETE ON question
FOR EACH ROW EXECUTE FUNCTION set_updated_on_timestamp();

CREATE TRIGGER answer_set_updated_on_trigger
BEFORE UPDATE OR DELETE ON answer
FOR EACH ROW EXECUTE FUNCTION set_updated_on_timestamp();