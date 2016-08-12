drop function post_comment_scores(bigint, bigint)
;

CREATE OR REPLACE FUNCTION post_comment_scores(postId BIGINT, rankId INT)
   RETURNS REFCURSOR AS
$BODY$
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
                          INNER JOIN post_comment_score pcs
                          ON pc.parent_id = pcs.id
                          WHERE pc.parent_id = pcs.id
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
$BODY$
LANGUAGE plpgsql
;