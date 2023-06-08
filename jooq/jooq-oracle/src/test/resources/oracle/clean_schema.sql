DELETE FROM user_vote;
DELETE FROM blog_user;
DELETE FROM post_tag;
DELETE FROM tag;
DELETE FROM post_details;
DELETE FROM post_comment_details;
DELETE FROM post_comment;
DELETE FROM post;

ALTER SEQUENCE hibernate_sequence RESTART START WITH 1;