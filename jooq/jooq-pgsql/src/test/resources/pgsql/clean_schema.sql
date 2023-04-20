DELETE FROM post_tag;
DELETE FROM tag;
DELETE FROM post_details;
DELETE FROM post_comment_details;
DELETE FROM post_comment;
DELETE FROM post;
DELETE FROM answer;
DELETE FROM question;

ALTER SEQUENCE hibernate_sequence RESTART;