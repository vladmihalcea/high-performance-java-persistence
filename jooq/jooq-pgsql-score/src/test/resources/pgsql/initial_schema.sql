drop table if exists post cascade;
drop table if exists post_comment cascade;

drop sequence hibernate_sequence;

create sequence hibernate_sequence start 1 increment 1;

create table post (id int8 not null, title varchar(255), primary key (id));
create table post_comment (id int8 not null, created_on timestamp, review varchar(255), score int4 not null, parent_id int8, post_id int8, primary key (id));

alter table post_comment add constraint FKmqxhu8q0j94rcly3yxlv0u498 foreign key (parent_id) references post_comment;
alter table post_comment add constraint post_comment_post_id foreign key (post_id) references post;

drop function if exists get_post_comment_scores;

CREATE OR REPLACE FUNCTION get_post_comment_scores(postId bigint, rankId integer)
RETURNS REFCURSOR AS
$$
DECLARE
    postComments REFCURSOR;
BEGIN
OPEN postComments FOR
SELECT id, parent_id, review, created_on, score
FROM (
    SELECT
     id, parent_id, review, created_on, score,
     dense_rank() OVER (ORDER BY total_score DESC) rank
    FROM (
        SELECT
            id, parent_id, review, created_on, score,
            SUM(score) OVER (PARTITION BY root_id) total_score
        FROM (
            WITH RECURSIVE post_comment_score(id, root_id, post_id,
                parent_id, review, created_on, score) AS (
                SELECT
                    id, id, post_id, parent_id, review, created_on, score
                    FROM post_comment
                    WHERE post_id = postId AND parent_id IS NULL
                    UNION ALL
                    SELECT pc.id, pcs.root_id, pc.post_id, pc.parent_id,
                    pc.review, pc.created_on, pc.score
                    FROM post_comment pc
                    INNER JOIN post_comment_score pcs ON pc.parent_id = pcs.id
                )
                SELECT id, parent_id, root_id, review, created_on, score
                FROM post_comment_score
        ) score_by_comment
    ) score_total
    ORDER BY total_score DESC, id ASC
) total_score_group
WHERE rank <= rankId;
RETURN postComments;
END;
$$
language 'plpgsql'
;
