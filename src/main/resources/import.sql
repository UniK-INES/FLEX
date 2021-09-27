--
-- PostgreSQL database Setup script to provide FLEX Market Product Patterns
-- Author: Sascha Holzhauer, 18.09.2017, update 02.04.2020
--

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DELETE FROM users_roles;


DELETE FROM roles_privileges;

-- Privileges
DELETE FROM Privilege;

INSERT INTO Privilege(id, name) VALUES (1, 'ROLE_admin');
INSERT INTO Privilege(id, name) VALUES (2, 'ROLE_write');
INSERT INTO Privilege(id, name) VALUES (3, 'ROLE_read');
INSERT INTO Privilege(id, name) VALUES (4, 'ROLE_inspect');


-- Roles
DELETE FROM Role;

INSERT INTO Role(id, name) VALUES (1, 'ADMIN');
INSERT INTO Role(id, name) VALUES (2, 'USER');
INSERT INTO Role(id, name) VALUES (3, 'INSPECTOR');

--
-- User accounts
--
DELETE FROM user_account;

INSERT INTO user_account(id, name, password, location) VALUES (1, 'admin', crypt('multimodalES',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer01_Node00');
INSERT INTO user_account(id, name, password, location) VALUES (2, 'flex1', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer01_Node01');
INSERT INTO user_account(id, name, password, location) VALUES (3, 'flex2', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node01');
INSERT INTO user_account(id, name, password, location) VALUES (11, 'node01', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node01');
INSERT INTO user_account(id, name, password, location) VALUES (12, 'node02', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node02');
INSERT INTO user_account(id, name, password, location) VALUES (13, 'node03', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node03');
INSERT INTO user_account(id, name, password, location) VALUES (14, 'node04', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node04');
INSERT INTO user_account(id, name, password, location) VALUES (15, 'node05', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node05');
INSERT INTO user_account(id, name, password, location) VALUES (16, 'node06', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node06');
INSERT INTO user_account(id, name, password, location) VALUES (17, 'node07', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node07');
INSERT INTO user_account(id, name, password, location) VALUES (18, 'node08', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node08');
INSERT INTO user_account(id, name, password, location) VALUES (19, 'node09', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node09');
INSERT INTO user_account(id, name, password, location) VALUES (20, 'node10', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node10');
INSERT INTO user_account(id, name, password, location) VALUES (21, 'node11', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node11');
INSERT INTO user_account(id, name, password, location) VALUES (22, 'node12', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node12');
INSERT INTO user_account(id, name, password, location) VALUES (23, 'node13', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node13');
INSERT INTO user_account(id, name, password, location) VALUES (24, 'node14', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node14');
INSERT INTO user_account(id, name, password, location) VALUES (25, 'node15', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node15');
INSERT INTO user_account(id, name, password, location) VALUES (26, 'node16', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node16');
INSERT INTO user_account(id, name, password, location) VALUES (27, 'node17', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node17');
INSERT INTO user_account(id, name, password, location) VALUES (28, 'node18', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node18');
INSERT INTO user_account(id, name, password, location) VALUES (29, 'node19', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node19');
INSERT INTO user_account(id, name, password, location) VALUES (30, 'node20', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node20');
INSERT INTO user_account(id, name, password, location) VALUES (31, 'node21', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node21');
INSERT INTO user_account(id, name, password, location) VALUES (32, 'node22', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node22');
INSERT INTO user_account(id, name, password, location) VALUES (33, 'node23', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node23');
INSERT INTO user_account(id, name, password, location) VALUES (34, 'node24', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node24');
INSERT INTO user_account(id, name, password, location) VALUES (35, 'node25', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node25');
INSERT INTO user_account(id, name, password, location) VALUES (36, 'node26', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node26');
INSERT INTO user_account(id, name, password, location) VALUES (37, 'node27', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node27');
INSERT INTO user_account(id, name, password, location) VALUES (38, 'node28', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node28');
INSERT INTO user_account(id, name, password, location) VALUES (39, 'node29', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node29');
INSERT INTO user_account(id, name, password, location) VALUES (40, 'node30', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node30');
INSERT INTO user_account(id, name, password, location) VALUES (41, 'node31', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node31');
INSERT INTO user_account(id, name, password, location) VALUES (42, 'node32', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node32');
INSERT INTO user_account(id, name, password, location) VALUES (43, 'node33', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node33');
INSERT INTO user_account(id, name, password, location) VALUES (44, 'node34', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node34');
INSERT INTO user_account(id, name, password, location) VALUES (45, 'node35', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node35');
INSERT INTO user_account(id, name, password, location) VALUES (46, 'node36', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node36');
INSERT INTO user_account(id, name, password, location) VALUES (47, 'node37', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node37');
INSERT INTO user_account(id, name, password, location) VALUES (48, 'node38', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node38');
INSERT INTO user_account(id, name, password, location) VALUES (49, 'node39', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node39');
INSERT INTO user_account(id, name, password, location) VALUES (50, 'node40', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node40');
INSERT INTO user_account(id, name, password, location) VALUES (51, 'node41', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node41');
INSERT INTO user_account(id, name, password, location) VALUES (52, 'node42', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node42');
INSERT INTO user_account(id, name, password, location) VALUES (53, 'node43', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node43');
INSERT INTO user_account(id, name, password, location) VALUES (54, 'node44', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node44');
INSERT INTO user_account(id, name, password, location) VALUES (55, 'node45', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node45');
INSERT INTO user_account(id, name, password, location) VALUES (56, 'node46', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node46');
INSERT INTO user_account(id, name, password, location) VALUES (57, 'node47', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node47');
INSERT INTO user_account(id, name, password, location) VALUES (58, 'node48', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node48');
INSERT INTO user_account(id, name, password, location) VALUES (59, 'node49', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node49');
INSERT INTO user_account(id, name, password, location) VALUES (60, 'node50', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node50');
INSERT INTO user_account(id, name, password, location) VALUES (61, 'node51', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node51');
INSERT INTO user_account(id, name, password, location) VALUES (62, 'node52', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node52');
INSERT INTO user_account(id, name, password, location) VALUES (63, 'node53', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node53');
INSERT INTO user_account(id, name, password, location) VALUES (64, 'node54', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node54');
INSERT INTO user_account(id, name, password, location) VALUES (65, 'node55', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node55');
INSERT INTO user_account(id, name, password, location) VALUES (66, 'node56', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node56');
INSERT INTO user_account(id, name, password, location) VALUES (67, 'node57', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node57');
INSERT INTO user_account(id, name, password, location) VALUES (68, 'node58', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node58');
INSERT INTO user_account(id, name, password, location) VALUES (69, 'node59', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node59');
INSERT INTO user_account(id, name, password, location) VALUES (70, 'node60', crypt('flex!',gen_salt('bf', 10)), '11X-KS-DIENST1-4_Transformer02_Node60');



-- Join users with roles
INSERT INTO users_roles(user_id, role_id) VALUES (1, 1);
INSERT INTO users_roles(user_id, role_id) VALUES (2, 2);
INSERT INTO users_roles(user_id, role_id) VALUES (3, 3);

INSERT INTO users_roles(user_id, role_id) VALUES (11, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (12, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (13, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (14, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (15, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (16, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (17, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (18, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (19, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (20, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (21, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (22, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (23, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (24, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (25, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (26, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (27, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (28, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (29, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (30, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (31, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (32, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (33, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (34, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (35, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (36, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (37, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (38, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (39, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (40, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (41, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (42, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (43, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (44, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (45, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (46, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (47, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (48, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (49, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (50, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (51, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (52, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (53, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (54, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (55, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (56, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (57, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (58, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (59, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (60, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (61, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (62, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (63, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (64, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (65, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (66, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (67, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (68, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (69, 3);
INSERT INTO users_roles(user_id, role_id) VALUES (70, 3);


-- Join privileges with roles
INSERT INTO roles_privileges(privilege_id, role_id) VALUES (1, 1);
INSERT INTO roles_privileges(privilege_id, role_id) VALUES (2, 1);
INSERT INTO roles_privileges(privilege_id, role_id) VALUES (3, 1);
INSERT INTO roles_privileges(privilege_id, role_id) VALUES (4, 1);
INSERT INTO roles_privileges(privilege_id, role_id) VALUES (2, 2);
INSERT INTO roles_privileges(privilege_id, role_id) VALUES (3, 2);
INSERT INTO roles_privileges(privilege_id, role_id) VALUES (4, 3);
INSERT INTO roles_privileges(privilege_id, role_id) VALUES (3, 3);

--
-- market_information
--
INSERT INTO market_information VALUES (1, 10.0, 3000.0);

--
-- market_product_pattern
--

-- TODO this is not understood by the Spring Boot parser (ArrayIndexOutOfScope)
-- https://www.postgresql.org/docs/9.1/static/sql-do.html

--DO $test$
--DECLARE starttime BIGINT;
--BEGIN
--	starttime := (SELECT (EXTRACT(EPOCH FROM date_trunc('second', now()))+5)*1000);
--	INSERT INTO market_product_pattern VALUES (1, '1m', '-1m', 100000, 0, starttime, '-5m');
--	INSERT INTO market_product_pattern VALUES (2, '1m', '-1m', 300000, 0, starttime, '-5m');
--	INSERT INTO market_product_pattern VALUES (3, '1m', '-1m', 600000, 0, starttime, '-5m');
--END $test$;

-- Work around:
DROP TABLE IF EXISTS temp_starttime ;
CREATE TEMPORARY TABLE temp_starttime (starttime BIGINT);

-- now plus 1 minute, converted to millis
-- INSERT INTO temp_starttime VALUES((SELECT (EXTRACT(EPOCH FROM date_trunc('minute', now()))+60)*1000));
-- start of next day (0:00h), converted to millis

--INSERT INTO temp_starttime VALUES((SELECT (EXTRACT(EPOCH FROM date_trunc('day', now() AT TIME ZONE 'Europe/Berlin'))+60*60*24)*1000));
INSERT INTO temp_starttime VALUES((SELECT (EXTRACT(EPOCH FROM date_trunc('day', to_timestamp(1596232800000/1000)))+60*60*24)*1000));

DELETE FROM mmarket_product_pattern;
DELETE FROM market_product_pattern;

INSERT INTO market_product_pattern(product_id, auction_interval,closing_time,delivery_period_duration,auction_delivery_span,energy_resolutionkwh,first_delivery_period_start,opening_time,min_price,max_price) VALUES (1, '1m', '-1m',  60000, 60000, 0, (SELECT starttime FROM temp_starttime), '-5m', -3000, 3000);
INSERT INTO market_product_pattern(product_id, auction_interval,closing_time,delivery_period_duration,auction_delivery_span,energy_resolutionkwh,first_delivery_period_start,opening_time,min_price,max_price) VALUES (2, '2m', '-2m', 900000, 900000, 0, (SELECT starttime FROM temp_starttime), '-15m', -3000, 3000);
INSERT INTO market_product_pattern(product_id, auction_interval,closing_time,delivery_period_duration,auction_delivery_span,energy_resolutionkwh,first_delivery_period_start,opening_time,min_price,max_price) VALUES (3, '10h', '-2h', 900000, 86400000, 0, (SELECT starttime FROM temp_starttime), '-12h', -3000, 3000);
INSERT INTO market_product_pattern(product_id, auction_interval,closing_time,delivery_period_duration,auction_delivery_span,energy_resolutionkwh,first_delivery_period_start,opening_time,min_price,max_price) VALUES (4, '22h', '-2h', 900000, 86400000, 0, (SELECT starttime FROM temp_starttime), '-24h', -3000, 3000);

--INSERT INTO market_product_pattern VALUES (3, '1m', '-1m', 600000, 0, (SELECT starttime FROM temp_starttime), '-5m', -3000, 3000);

--
-- mmarket_product_pattern
--
INSERT INTO mmarket_product_pattern(mmproduct_id,active,clearing_id,description,product_pattern_product_id) VALUES (1, false, 'UNIFORM', 'Uniform, 1min', 1);
INSERT INTO mmarket_product_pattern(mmproduct_id,active,clearing_id,description,product_pattern_product_id) VALUES (2, false, 'TESTING', 'Testing', 2);
INSERT INTO mmarket_product_pattern(mmproduct_id,active,clearing_id,description,product_pattern_product_id) VALUES (3, false, 'MATCHING_CLOSED_SIMPLE', 'Simple Day Ahead Matching', 3);
INSERT INTO mmarket_product_pattern(mmproduct_id,active,clearing_id,description,product_pattern_product_id) VALUES (4, true, 'MATCHING_CLOSED_ACTIVATION', 'Long Open Day Ahead Matching', 4);

--
-- clearing infos
--
DELETE FROM clearing_info;

------------------------------------------------
---- REQUESTS ----------------------------------
------------------------------------------------

CREATE TABLE IF NOT EXISTS requests_status_names (id integer NOT NULL PRIMARY KEY, name character varying(255), description character varying(255));

INSERT INTO requests_status_names VALUES (0, 'UNHANDLED', 'Not cleared') ON CONFLICT DO NOTHING;
INSERT INTO requests_status_names VALUES (1, 'ACCEPTED', 'Accepted wehn cleared') ON CONFLICT DO NOTHING;
INSERT INTO requests_status_names VALUES (2, 'PARTLY_ACCEPTED', 'Partly accepted when cleared') ON CONFLICT DO NOTHING;
INSERT INTO requests_status_names VALUES (3, 'DECLINED', 'Not accepted when cleared') ON CONFLICT DO NOTHING;
INSERT INTO requests_status_names VALUES (6, 'INVALID', 'Not valid') ON CONFLICT DO NOTHING;
INSERT INTO requests_status_names VALUES (7, 'INVALID_TOO_EARLY', 'Request sent too early (before opening time)') ON CONFLICT DO NOTHING;
INSERT INTO requests_status_names VALUES (8, 'INVALID_TOO_LATE', 'Request sent too late (after closing time)') ON CONFLICT DO NOTHING;
INSERT INTO requests_status_names VALUES (9, 'INVALID_STARTTIME', 'Starttime not valid') ON CONFLICT DO NOTHING;
INSERT INTO requests_status_names VALUES (10, 'INVALID_ENDTIME', 'Endtime not valid') ON CONFLICT DO NOTHING;
INSERT INTO requests_status_names VALUES (11, 'INVALID_PRODUCT', 'Invalid product') ON CONFLICT DO NOTHING;
INSERT INTO requests_status_names VALUES (12, 'INVALID_PRICE', 'Invalid price (exceeding product limits)') ON CONFLICT DO NOTHING;
INSERT INTO requests_status_names VALUES (13, 'INVALID_ENERGY', 'Invalid energy (not sticking to resolution)') ON CONFLICT DO NOTHING;

------------------------------------------------
---- SECURITY ----------------------------------
------------------------------------------------
drop table if exists oauth_client_details;
create table oauth_client_details (client_id VARCHAR(255) PRIMARY KEY, resource_ids VARCHAR(255), client_secret VARCHAR(255), scope VARCHAR(255), authorized_grant_types VARCHAR(255), web_server_redirect_uri VARCHAR(255), authorities VARCHAR(255), access_token_validity INTEGER, refresh_token_validity INTEGER, additional_information VARCHAR(4096), autoapprove VARCHAR(255));
 
drop table if exists oauth_client_token;
create table oauth_client_token (token_id VARCHAR(255), token BYTEA, authentication_id VARCHAR(255) PRIMARY KEY, user_name VARCHAR(255), client_id VARCHAR(255)); 

drop table if exists oauth_access_token;
create table oauth_access_token (token_id VARCHAR(255), token BYTEA, authentication_id VARCHAR(255) PRIMARY KEY, user_name VARCHAR(255), client_id VARCHAR(255), authentication BYTEA, refresh_token VARCHAR(255));
 
drop table if exists oauth_refresh_token;
create table oauth_refresh_token (token_id VARCHAR(255), token BYTEA, authentication BYTEA);
 
drop table if exists oauth_code;
create table oauth_code (code VARCHAR(255), authentication BYTEA);
 
drop table if exists oauth_approvals;
create table oauth_approvals (userId VARCHAR(255), clientId VARCHAR(255), scope VARCHAR(255), status VARCHAR(10), expiresAt TIMESTAMP, lastModifiedAt TIMESTAMP);
 
drop table if exists ClientDetails;
create table ClientDetails (appId VARCHAR(255) PRIMARY KEY, resourceIds VARCHAR(255), appSecret VARCHAR(255), scope VARCHAR(255), grantTypes VARCHAR(255), redirectUrl VARCHAR(255), authorities VARCHAR(255), access_token_validity INTEGER, refresh_token_validity INTEGER, additionalInformation VARCHAR(4096), autoApproveScopes VARCHAR(255));


DELETE FROM oauth_client_details;
INSERT INTO oauth_client_details (client_id, client_secret, scope, authorized_grant_types, web_server_redirect_uri, authorities, access_token_validity,	refresh_token_validity, additional_information, autoapprove) VALUES ('FlexAdmin', crypt('multimodalES',gen_salt('bf', 10)), 'admin,read,write', 'password,authorization_code,refresh_token', null, null, 36000, 36000, null, true);
INSERT INTO oauth_client_details (client_id, client_secret, scope, authorized_grant_types, web_server_redirect_uri, authorities, access_token_validity,	refresh_token_validity, additional_information, autoapprove) VALUES ('FlexUser', crypt('flex!',gen_salt('bf', 10)), 'read,write', 'password,authorization_code,refresh_token', null, null, 36000, 36000, null, false);
