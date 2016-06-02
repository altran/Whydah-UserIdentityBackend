DROP TABLE Roles IF EXISTS;
DROP TABLE Applications IF EXISTS;

CREATE TABLE Application (
  id varchar(64),
  json text,
  PRIMARY KEY(id)
);