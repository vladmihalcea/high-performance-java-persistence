create table post_details (id int8 not null, created_by varchar(255), created_on timestamp, primary key (id));

alter table post_details add constraint POST_DETAILS_POST_ID_FK foreign key (id) references post;