DROP TABLE IF EXISTS Roles;
DROP TABLE IF EXISTS Applications;

CREATE TABLE Application (
  id varchar(64),
  json text,
  PRIMARY KEY(id)
);