CREATE SEQUENCE user_seq
    START 1 INCREMENT 1;

CREATE TABLE users (
    id bigint NOT NULL,
    name varchar(255),
    PRIMARY KEY (id)
);