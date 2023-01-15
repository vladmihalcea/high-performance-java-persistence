CREATE SEQUENCE post_seq
START 1 INCREMENT 1;

CREATE TABLE post (
    id int8 NOT NULL,
    title varchar(255),
    PRIMARY KEY (id)
);

CREATE SEQUENCE tag_seq
    START 1 INCREMENT 1;

CREATE TABLE tag (
    id int8 NOT NULL,
    name varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE post_tag (
    post_id int8 NOT NULL,
    tag_id int8 NOT NULL,
    PRIMARY KEY (post_id, tag_id)
);

ALTER TABLE post_tag
ADD CONSTRAINT POST_TAG_TAG_ID_FK
FOREIGN KEY (tag_id) REFERENCES tag;

ALTER TABLE post_tag
ADD CONSTRAINT POST_TAG_POST_ID_FK
FOREIGN KEY (post_id) REFERENCES post;