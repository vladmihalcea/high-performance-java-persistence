IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_insert_post_audit_log' and type = 'TR')
DROP TRIGGER tr_insert_post_audit_log;

IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_update_post_audit_log' and type = 'TR')
DROP TRIGGER tr_update_post_audit_log;

IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_delete_post_audit_log' and type = 'TR')
DROP TRIGGER tr_delete_post_audit_log;

IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_insert_post_comment_audit_log' and type = 'TR')
DROP TRIGGER tr_insert_post_comment_audit_log;

IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_update_post_comment_audit_log' and type = 'TR')
DROP TRIGGER tr_update_post_comment_audit_log;

IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_delete_post_comment_audit_log' and type = 'TR')
DROP TRIGGER tr_delete_post_comment_audit_log;

IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_insert_post_details_audit_log' and type = 'TR')
DROP TRIGGER tr_insert_post_details_audit_log;

IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_update_post_details_audit_log' and type = 'TR')
DROP TRIGGER tr_update_post_details_audit_log;

IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_delete_post_details_audit_log' and type = 'TR')
DROP TRIGGER tr_delete_post_details_audit_log;

IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_insert_post_tag_audit_log' and type = 'TR')
DROP TRIGGER tr_insert_post_tag_audit_log;

IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_update_post_tag_audit_log' and type = 'TR')
DROP TRIGGER tr_update_post_tag_audit_log;

IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_delete_post_tag_audit_log' and type = 'TR')
DROP TRIGGER tr_delete_post_tag_audit_log;

IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_insert_tag_audit_log' and type = 'TR')
DROP TRIGGER tr_insert_tag_audit_log;

IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_update_tag_audit_log' and type = 'TR')
DROP TRIGGER tr_update_tag_audit_log;

IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_delete_tag_audit_log' and type = 'TR')
DROP TRIGGER tr_delete_tag_audit_log;

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