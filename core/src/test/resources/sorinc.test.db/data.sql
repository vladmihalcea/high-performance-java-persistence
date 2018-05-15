-- nothing for moment

-- random_uuid()
-- BINARY(1000)
insert into account(first_name, last_name, mid_name, username, password, email, phone, type, country, county, town, street, zip_code) values('f_n', 'l_n', 'm_n', 'username_1', 'password_1', 'nobody@fake.net', '00401234567891', 'business', 'Ro', 'Cj', 'cluj-napoca', 'Garii 21', '400345');
insert into account(first_name, last_name, mid_name, username, password, email, phone, type, country, county, town, street, zip_code) values('f_n', 'l_n', 'm_n', 'username_2', 'password_2', 'nobody@fake.net', '00401234567891', 'business', 'Ro', 'Cj', 'cluj-napoca', 'Garii 21', '400345');


insert into token(reference, jwt, account_id) values('dd127237-e266-45fd-8a05-a26c7ee54371', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c', 1);
insert into token(reference, jwt, account_id) values('70b5f071-129a-4b3b-be50-1a9569b9ef7e', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c', 2);