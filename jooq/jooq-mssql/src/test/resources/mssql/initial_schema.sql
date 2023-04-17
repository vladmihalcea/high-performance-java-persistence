IF EXISTS (SELECT * FROM sysobjects WHERE name='post_tag' and xtype='U')
DROP TABLE post_tag;

IF EXISTS (SELECT * FROM sysobjects WHERE name='tag' and xtype='U')
DROP TABLE tag;

IF EXISTS (SELECT * FROM sysobjects WHERE name='post_details' and xtype='U')
DROP TABLE post_details;

IF EXISTS (SELECT * FROM sysobjects WHERE name='post_comment' and xtype='U')
DROP TABLE post_comment;

IF EXISTS (SELECT * FROM sysobjects WHERE name='post' and xtype='U')
DROP TABLE post;

IF EXISTS (SELECT * FROM sysobjects WHERE name='post_audit_log' and xtype='U')
DROP TABLE post_audit_log;

IF EXISTS (SELECT * FROM sysobjects WHERE name='hibernate_sequence' and xtype='SO')
DROP SEQUENCE hibernate_sequence;

CREATE TABLE post (id bigint not null, title varchar(255), primary key (id));
CREATE TABLE post_comment (id bigint not null, review varchar(255), post_id bigint, primary key (id));
CREATE TABLE post_details (id bigint not null, created_by varchar(255), created_on datetime2, updated_by varchar(255), updated_on datetime2, primary key (id));
CREATE TABLE tag (id bigint not null, name varchar(255), primary key (id));
CREATE TABLE post_tag (post_id bigint not null, tag_id bigint not null);

ALTER TABLE post_comment ADD CONSTRAINT post_comment_post_id FOREIGN KEY (post_id) REFERENCES post;
ALTER TABLE post_details ADD CONSTRAINT post_details_id FOREIGN KEY (id) REFERENCES post;
ALTER TABLE post_tag ADD CONSTRAINT post_tag_tag_id FOREIGN KEY (tag_id) REFERENCES Tag;
ALTER TABLE post_tag ADD CONSTRAINT post_tag_post_id FOREIGN KEY (post_id) REFERENCES post;

CREATE SEQUENCE hibernate_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE post_audit_log (
  post_id bigint NOT NULL,
  old_row_data nvarchar(1000) CHECK(ISJSON(old_row_data) = 1),
  new_row_data nvarchar(1000) CHECK(ISJSON(new_row_data) = 1),
  dml_type varchar(10) NOT NULL CHECK (dml_type IN ('INSERT', 'UPDATE', 'DELETE')),
  dml_timestamp datetime NOT NULL,
  dml_createdby varchar(255) NOT NULL,
  trx_timestamp datetime NOT NULL,
  PRIMARY KEY (post_id, dml_type, dml_timestamp)
);
