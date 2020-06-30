CREATE TABLE post_details (
    id int8 NOT NULL,
    created_by varchar(255),
    created_on TIMESTAMP,
    PRIMARY KEY (id)
);

ALTER TABLE post_details
ADD CONSTRAINT POST_DETAILS_POST_ID_FK
FOREIGN KEY (id) REFERENCES post;