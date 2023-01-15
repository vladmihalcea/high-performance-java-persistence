CREATE SEQUENCE post_comment_seq
    START 1 INCREMENT 1;

CREATE TABLE post_comment (
    id int8 NOT NULL,
    review varchar(255),
    post_id int8, PRIMARY KEY (id)
);

ALTER TABLE post_comment
ADD CONSTRAINT POST_COMMENT_POST_ID_FK
FOREIGN KEY (post_id) REFERENCES post;