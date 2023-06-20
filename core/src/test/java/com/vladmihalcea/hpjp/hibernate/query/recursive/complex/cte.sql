
WITH RECURSIVE post_comment_score(post_review, quantity) AS (
    SELECT sub_part, part, quantity FROM parts WHERE part = 'our_product'
  UNION ALL
    SELECT post_id, post_comment_id, review,
    FROM post_comment_score pr, parts p
    WHERE p.part = pr.sub_part
  )

SELECT id, post_id, post_comment_id, review, SUM(score) as total_score
FROM post_comment_score
GROUP BY post_id, post_comment_id, review

---------------

select *
from post_comment pc
left join post_comment_vote pcv on pcv.comment_id = pc.id
where pcv.comment_id is null




-------------

SELECT
    id, post_id, parent_id, review, 0 as score
FROM post_comment
WHERE parent_id is null and post_id = 1

-------


WITH RECURSIVE post_comment_score(id, post_id, parent_id, review, score) AS (
    SELECT
        id, post_id, parent_id, review, 0 as score
    FROM post_comment
    WHERE parent_id is null
  UNION ALL
    select
        id, post_id, parent_id, review, score, sum(score) as total_score
    from post_comment_score pcs, (
        select
            pc.id, pc.post_id, pc.parent_id, review,
            (
                case when pcv.up is null
                then 0
                else
                (
                    case when pcv.up = true
                    then 1
                    else -1
                    end
                )
                end
            ) as score
        from post_comment pc
        left join post_comment_vote pcv on pcv.comment_id = pc.id
        where pc.parent_id = pcs.id
    ) post_comment_score
    group by
        id, post_id, parent_id, review, score
)
SELECT id, post_id, parent_id, review, score
FROM post_comment_score
WHERE post_id = 1


---------------


WITH RECURSIVE post_comment_score(id, post_id, parent_id, review, score) AS (
    SELECT
        id, post_id, parent_id, review, 0 as score
    FROM post_comment
    WHERE parent_id is null
  UNION ALL
    select
        pc.id, pc.post_id, pc.parent_id, pc.review,
        (
            case when pcv.up is null
            then 0
            else
            (
                case when pcv.up = true
                then 1
                else -1
                end
            )
            end
        ) as score
    from post_comment pc, post_comment_score pcs
    left join post_comment_vote pcv on pcv.comment_id = pc.id
    where pc.parent_id = pcs.id
)
SELECT id, post_id, parent_id, review, score
FROM post_comment_score
WHERE post_id = 1

-------------

select
    comment_id,
    sum (
        case when pcv.up is null
        then 0
        else
        (
            case when pcv.up = true
            then 1
            else -1
            end
        )
        end
    ) as score
from post_comment_vote pcv
group by comment_id



---------------


select
pc.id, pc.post_id, pc.parent_id, pc.review,
(
    select
    sum (
        case when pcv.up is null
        then 0
        else
        (
            case when pcv.up = true
            then 1
            else -1
            end
        )
        end
    ) as score
    from post_comment_vote pcv
    where pcv.comment_id = pc.id
) as score
from post_comment pc

---------


WITH RECURSIVE post_comment_score(id, root_id, post_id, parent_id, review, score) AS (
    SELECT
        id, id, post_id, parent_id, review, 0 as score
    FROM post_comment
    WHERE parent_id is null
  UNION ALL
    select
        pc.id, pcs.root_id, pc.post_id, pc.parent_id, pc.review,
        (
            case when pcv.up is null
            then 0
            else
            (
                case when pcv.up = true
                then 1
                else -1
                end
            )
            end
        ) as score
    from post_comment pc
    left join post_comment_vote pcv on pcv.comment_id = pc.id
    join post_comment_score pcs on pc.parent_id = pcs.id
)
SELECT root_id, review, sum(score) as total_score
FROM post_comment_score
WHERE post_id = 1
group by root_id, review


---------------------

WITH RECURSIVE post_comment_score(id, root_id, post_id, parent_id, review, score) AS (
    SELECT
        id, id, post_id, parent_id, review, 0 as score
    FROM post_comment
    WHERE parent_id is null
  UNION ALL
    select
        pc.id, pcs.root_id, pc.post_id, pc.parent_id, pc.review,
        (
            case when pcv.up is null
            then 0
            else
            (
                case when pcv.up = true
                then 1
                else -1
                end
            )
            end
        ) as score
    from post_comment pc
    left join post_comment_vote pcv on pcv.comment_id = pc.id
    join post_comment_score pcs on pc.parent_id = pcs.id
)
SELECT distinct id, review, sum(score) OVER (PARTITION BY root_id) as total_score
FROM post_comment_score
WHERE post_id = 1
order by total_score desc
limit 6

------

select id, parent_id, root_id, review, created_on, score, sum(score) over w as total_score
from (
    WITH RECURSIVE post_comment_score(id, root_id, post_id, parent_id, review, created_on, score) AS (
        SELECT
            id, id, post_id, parent_id, review, created_on, 0 as score
        FROM post_comment
        WHERE parent_id is null
      UNION ALL
        select
            pc.id, pcs.root_id, pc.post_id, pc.parent_id, pc.review, pc.created_on,
            (
                case when pcv.up is null
                then 0
                else
                (
                    case when pcv.up = true
                    then 1
                    else -1
                    end
                )
                end
            ) as score
        from post_comment pc
        left join post_comment_vote pcv on pcv.comment_id = pc.id
        join post_comment_score pcs on pc.parent_id = pcs.id
    )
    SELECT distinct id, parent_id, root_id, review, created_on, score
    FROM post_comment_score
    WHERE post_id = 1
) as total_scores
WINDOW w AS (PARTITION BY root_id)
order by total_score desc, created_on asc

------------------------------------------------

select distinct id, parent_id, root_id, review, created_on, total_score
from (
    WITH RECURSIVE post_comment_score(id, root_id, post_id, parent_id, review, created_on, score) AS (
        SELECT
            id, id, post_id, parent_id, review, created_on, 0 as score
        FROM post_comment
        WHERE parent_id is null
      UNION ALL
        select
            pc.id, pcs.root_id, pc.post_id, pc.parent_id, pc.review, pc.created_on,
            (
                case when pcv.up is null
                then 0
                else
                (
                    case when pcv.up = true
                    then 1
                    else -1
                    end
                )
                end
            ) as score
        from post_comment pc
        left join post_comment_vote pcv on pcv.comment_id = pc.id
        join post_comment_score pcs on pc.parent_id = pcs.id
    )
    SELECT id, parent_id, root_id, review, created_on, score, sum(score) over w as total_score
    FROM post_comment_score
    WHERE post_id = 1
    WINDOW w AS (PARTITION BY root_id)
    order by total_score desc, created_on asc
) as total_scores
where total_score > 0

---------


SELECT DISTINCT id ,parent_id ,root_id ,review ,created_on ,total_score
FROM (
    WITH RECURSIVE post_comment_score(id, root_id, post_id, parent_id, review, created_on, score) AS (
        SELECT id ,id ,post_id ,parent_id ,review ,created_on ,0 AS score
        FROM post_comment
        WHERE parent_id IS NULL

        UNION ALL

        SELECT pc.id ,pcs.root_id ,pc.post_id ,pc.parent_id ,pc.review ,pc.created_on, (
            CASE
            WHEN pcv.up IS NULL
            THEN 0
            ELSE (
                CASE
                WHEN pcv.up = true
                THEN 1
                ELSE - 1
                END
            )
            END
        ) AS score
        FROM post_comment pc
        LEFT JOIN post_comment_vote pcv ON pcv.comment_id = pc.id
        INNER JOIN post_comment_score pcs ON pc.parent_id = pcs.id
    )
    SELECT id ,parent_id ,root_id ,review ,created_on ,score ,sum(score) OVER w AS total_score
    FROM post_comment_score
    WHERE post_id = 1 WINDOW w AS (PARTITION BY root_id)
    ORDER BY total_score DESC
        ,created_on ASC
    ) AS total_scores
WHERE total_score > 0