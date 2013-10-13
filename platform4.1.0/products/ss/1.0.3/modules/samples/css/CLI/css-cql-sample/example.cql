CREATE KEYSPACE test_cql WITH strategy_class = 'SimpleStrategy' AND strategy_options:replication_factor = 1;

USE test_cql;

CREATE COLUMNFAMILY StandardLong1 (key text PRIMARY KEY,birthyear int,name text,age int) WITH comparator=UTF8Type AND default_validation=UTF8Type

INSERT INTO StandardLong1 (KEY, birthyear,name,age) VALUES ('cqlQueryTest_key1',1975,'tom',24);
INSERT INTO StandardLong1 (KEY, birthyear,name,age)  VALUES ('cqlQueryTest_key2',1976,'jerry',25);
INSERT INTO StandardLong1 (KEY, birthyear,name,age) VALUES ('cqlQueryTest_key3',1977,'peter',26);
INSERT INTO StandardLong1 (KEY, birthyear,name,age) VALUES ('cqlQueryTest_key4',1978,'olivia',27);
INSERT INTO StandardLong1 (KEY, birthyear,name,age)  VALUES ('cqlQueryTest_key5',1979,'walter',28);
INSERT INTO StandardLong1 (KEY, birthyear,name,age) VALUES ('cqlQueryTest_key6',1980,'nina',29);
INSERT INTO StandardLong1 (KEY, birthyear,name,age) VALUES ('cqlQueryTest_key7',1981,'bell',30);
INSERT INTO StandardLong1 (KEY, birthyear,name,age) VALUES ('cqlQueryTest_key8',1982,'cate',31);

SELECT name,age from StandardLong1;
SELECT COUNT(*) from StandardLong1;

SELECT name FROM StandardLong1 WHERE key IN (cqlQueryTest_key5,cqlQueryTest_key4,cqlQueryTest_key6);

UPDATE StandardLong1 SET name = tommy WHERE KEY = cqlQueryTest_key1
UPDATE StandardLong1 SET name = timmy WHERE KEY = cqlQueryTest_key2
UPDATE StandardLong1 SET name = gimmy WHERE KEY = cqlQueryTest_key3

DELETE name from StandardLong1 WHERE KEY='cqlQueryTest_key5'
