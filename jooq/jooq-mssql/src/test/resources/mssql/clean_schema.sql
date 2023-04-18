DELETE FROM post_tag;
DELETE FROM tag;
DELETE FROM post_details;
DELETE FROM post_comment;
DELETE FROM post;

ALTER SEQUENCE hibernate_sequence RESTART;

DELETE FROM post_audit_log;
DELETE FROM post_details_audit_log;
DELETE FROM post_comment_audit_log;
DELETE FROM post_tag_audit_log;
DELETE FROM tag_audit_log;