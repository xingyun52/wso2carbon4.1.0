-- WSO2 BAM DATABASE SQL FOR H2

--
-- TABLES
--

CREATE TABLE IF NOT EXISTS BAM_ACTIVITY (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_NAME VARCHAR(255),
  BAM_DESCRIPTION VARCHAR(1024) NOT NULL,
  BAM_USER_DEFINED_ID VARCHAR(36) NOT NULL,
  PRIMARY KEY (BAM_ID)
);

CREATE TABLE IF NOT EXISTS BAM_DAY_DIM (
  BAM_ID INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_START_TIMESTAMP TIMESTAMP NOT NULL ,
  BAM_NAME VARCHAR(24),
  BAM_DAY_OF_WEEK INT(11) NOT NULL,
  BAM_DAY_OF_MONTH INT(11) NOT NULL,
  BAM_DAY_OF_YEAR INT(11) NOT NULL,
  BAM_MONTH INT(11) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_ENDPOINT_STAT_DAY_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_ENDPOINT_NAME VARCHAR(512) NOT NULL,
  BAM_DIRECTION VARCHAR(10) NOT NULL,
  BAM_DAY_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
) ;

CREATE TABLE IF NOT EXISTS BAM_ENDPOINT_STAT_HOUR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_ENDPOINT_NAME VARCHAR(512) NOT NULL,
  BAM_DIRECTION VARCHAR(10) NOT NULL,
  BAM_HOUR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_ENDPOINT_STAT_MONTH_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_ENDPOINT_NAME VARCHAR(512) NOT NULL,
  BAM_DIRECTION VARCHAR(10) NOT NULL,
  BAM_MONTH_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_ENDPOINT_STAT_QTR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_ENDPOINT_NAME VARCHAR(512) NOT NULL,
  BAM_DIRECTION VARCHAR(10) NOT NULL,
  BAM_QTR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_ENDPOINT_STAT_YEAR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_ENDPOINT_NAME VARCHAR(512) NOT NULL,
  BAM_DIRECTION VARCHAR(10) NOT NULL,
  BAM_YEAR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_HOUR_DIM (
  BAM_ID INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_START_TIMESTAMP TIMESTAMP NOT NULL ,
  BAM_HOUR_NO INT(11) NOT NULL,
  BAM_DAY INT(11) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_MESSAGE (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_OP_ID INT(10) UNSIGNED NOT NULL,
  BAM_MSG_ID VARCHAR(45) NOT NULL COMMENT 'WS-ADDRESSING MESSAGE ID',
  BAM_ACTIVITY_ID INT(10) UNSIGNED DEFAULT NULL,
  BAM_TIMESTAMP DATETIME NOT NULL,
  BAM_IP_ADDRESS VARCHAR(45) NOT NULL,
  BAM_USER_AGENT VARCHAR(45) NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_MESSAGE_USER_DATA (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_MESSAGE_ID INT(10) UNSIGNED NOT NULL,
  BAM_TIMESTAMP DATETIME NOT NULL,
  BAM_KEY VARCHAR(255) NOT NULL,
  BAM_VALUE VARCHAR(1024) NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_MONTH_DIM (
  BAM_ID INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_START_TIMESTAMP TIMESTAMP NOT NULL ,
  BAM_NAME VARCHAR(24),
  BAM_NO INT(11) NOT NULL,
  BAM_QTR INT(11) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_OPERATION (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVICE_ID INT(10) UNSIGNED NOT NULL,
  BAM_OP_NAME VARCHAR(255) NOT NULL,
  BAM_DESCRIPTION VARCHAR(1024) DEFAULT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_OPERATION_DATA (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_OPERATION_ID INT(10) UNSIGNED NOT NULL,
  BAM_TIMESTAMP DATETIME NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_CUM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_CUM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_CUM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_OPERATION_STAT_DAY_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_OPERATION_ID INT(10) UNSIGNED NOT NULL,
  BAM_DAY_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_OPERATION_STAT_HOUR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_OPERATION_ID INT(10) UNSIGNED NOT NULL,
  BAM_HOUR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_OPERATION_STAT_MONTH_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_OPERATION_ID INT(10) UNSIGNED NOT NULL,
  BAM_MONTH_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_OPERATION_STAT_QTR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_OPERATION_ID INT(10) UNSIGNED NOT NULL,
  BAM_QTR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_OPERATION_STAT_YEAR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_OPERATION_ID INT(10) UNSIGNED NOT NULL,
  BAM_YEAR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_OPERATION_USER_DATA (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_OPERATION_ID INT(10) UNSIGNED NOT NULL,
  BAM_TIMESTAMP DATETIME NOT NULL,
  BAM_KEY VARCHAR(255) NOT NULL,
  BAM_VALUE VARCHAR(1024) NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_PROXY_STAT_DAY_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_PROXY_NAME VARCHAR(512) NOT NULL,
  BAM_DIRECTION VARCHAR(10) NOT NULL,
  BAM_DAY_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_PROXY_STAT_HOUR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_PROXY_NAME VARCHAR(512) NOT NULL,
  BAM_DIRECTION VARCHAR(10) NOT NULL,
  BAM_HOUR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_PROXY_STAT_MONTH_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_PROXY_NAME VARCHAR(512) NOT NULL,
  BAM_DIRECTION VARCHAR(10) NOT NULL,
  BAM_MONTH_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_PROXY_STAT_QTR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_PROXY_NAME VARCHAR(512) NOT NULL,
  BAM_DIRECTION VARCHAR(10) NOT NULL,
  BAM_QTR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_PROXY_STAT_YEAR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_PROXY_NAME VARCHAR(512) NOT NULL,
  BAM_DIRECTION VARCHAR(10) NOT NULL,
  BAM_YEAR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_QTR_DIM (
  BAM_ID INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_START_TIMESTAMP TIMESTAMP NOT NULL ,
  BAM_NAME VARCHAR(24),
  BAM_NO INT(11) NOT NULL,
  BAM_YEAR INT(11) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SEQUENCE_STAT_DAY_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_SEQUENCE_NAME VARCHAR(512) NOT NULL,
  BAM_DIRECTION VARCHAR(10) NOT NULL,
  BAM_DAY_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SEQUENCE_STAT_HOUR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_SEQUENCE_NAME VARCHAR(512) NOT NULL,
  BAM_DIRECTION VARCHAR(10) NOT NULL,
  BAM_HOUR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SEQUENCE_STAT_MONTH_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_SEQUENCE_NAME VARCHAR(512) NOT NULL,
  BAM_DIRECTION VARCHAR(10) NOT NULL,
  BAM_MONTH_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SEQUENCE_STAT_QTR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_SEQUENCE_NAME VARCHAR(512) NOT NULL,
  BAM_DIRECTION VARCHAR(10) NOT NULL,
  BAM_QTR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SEQUENCE_STAT_YEAR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_SEQUENCE_NAME VARCHAR(512) NOT NULL,
  BAM_DIRECTION VARCHAR(10) NOT NULL,
  BAM_YEAR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVER (
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_TENENT_ID INT(10) UNSIGNED DEFAULT NULL,
  BAM_TYPE VARCHAR(128) NOT NULL,
  BAM_CATEGORY INT(4) UNSIGNED DEFAULT NULL,
  BAM_URL VARCHAR(255) NOT NULL,
  BAM_ACTIVE TINYINT(1) NOT NULL DEFAULT '1',
  BAM_DESCRIPTION VARCHAR(1024) DEFAULT NULL,
  BAM_SUBSCRIPTION_ID VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (BAM_SERVER_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVER_DATA (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_TIMESTAMP DATETIME NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_CUM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_CUM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_CUM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
);

CREATE TABLE IF NOT EXISTS BAM_SERVER_LOGIN_DATA (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_TIMESTAMP DATETIME NOT NULL,
  BAM_CUM_LOGIN_ATTEMPTS INT(10) UNSIGNED NOT NULL,
  BAM_CUM_FAILED_LOGIN_ATTEMPTS INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVER_STAT_DAY_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_DAY_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVER_STAT_HOUR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_HOUR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVER_STAT_MONTH_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_MONTH_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVER_STAT_QTR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_QTR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVER_STAT_YEAR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_YEAR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVER_USER_DATA (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_TIMESTAMP DATETIME NOT NULL,
  BAM_KEY VARCHAR(255) NOT NULL,
  BAM_VALUE VARCHAR(1024) NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVER_USER_LOGIN_DATA (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_TIMESTAMP DATETIME NOT NULL,
  BAM_USER_NAME VARCHAR(45) NOT NULL,
  BAM_CUM_LOGIN_ATTEMPTS INT(10) UNSIGNED NOT NULL,
  BAM_CUM_FAILED_LOGIN_ATTEMPTS INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVICE (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_SERVICE_NAME VARCHAR(255) NOT NULL,
  BAM_DESCRIPTION VARCHAR(1024) DEFAULT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVICE_DATA (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVICE_ID INT(10) UNSIGNED NOT NULL,
  BAM_TIMESTAMP DATETIME NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_CUM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_CUM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_CUM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVICE_STAT_DAY_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVICE_ID INT(10) UNSIGNED NOT NULL,
  BAM_DAY_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVICE_STAT_HOUR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVICE_ID INT(10) UNSIGNED NOT NULL,
  BAM_HOUR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVICE_STAT_MONTH_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVICE_ID INT(10) UNSIGNED NOT NULL,
  BAM_MONTH_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVICE_STAT_QTR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVICE_ID INT(10) UNSIGNED NOT NULL,
  BAM_QTR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVICE_STAT_YEAR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVICE_ID INT(10) UNSIGNED NOT NULL,
  BAM_YEAR_ID INT(10) UNSIGNED NOT NULL,
  BAM_AVG_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MAX_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_MIN_RES_TIME DECIMAL(10,0) NOT NULL,
  BAM_REQ_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_RES_COUNT INT(10) UNSIGNED NOT NULL,
  BAM_FAULT_COUNT INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_SERVICE_USER_DATA (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVICE_ID INT(10) UNSIGNED NOT NULL,
  BAM_TIMESTAMP DATETIME NOT NULL,
  BAM_KEY VARCHAR(255) NOT NULL,
  BAM_VALUE VARCHAR(1024) NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_TENENT (
  BAM_TENENT_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_DESCRIPTION VARCHAR(1024) DEFAULT NULL,
  PRIMARY KEY (BAM_TENENT_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_YEAR_DIM (
  BAM_ID INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_START_TIMESTAMP TIMESTAMP NOT NULL ,
  BAM_NO INT(11) NOT NULL,
  PRIMARY KEY (BAM_ID)
); 

CREATE TABLE IF NOT EXISTS BAM_BANDWIDTH_STAT_HOUR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_HOUR_ID INT(10) UNSIGNED NOT NULL,
  BAM_BANDWIDTH_NAME VARCHAR(512) NOT NULL,
  BAM_INCOMING_BANDWIDTH INT(20) NOT NULL,
  BAM_OUTGOING_BANDWIDTH INT(20) NOT NULL,
  PRIMARY KEY (BAM_ID)
);
 
CREATE TABLE IF NOT EXISTS BAM_BANDWIDTH_STAT_DAY_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_DAY_ID INT(10) UNSIGNED NOT NULL,
  BAM_BANDWIDTH_NAME VARCHAR(512) NOT NULL,
  BAM_INCOMING_BANDWIDTH INT(20) NOT NULL,
  BAM_OUTGOING_BANDWIDTH INT(20) NOT NULL,
  PRIMARY KEY (BAM_ID)
) ;
 
CREATE TABLE IF NOT EXISTS BAM_BANDWIDTH_STAT_MONTH_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_MONTH_ID INT(10) UNSIGNED NOT NULL,
  BAM_BANDWIDTH_NAME VARCHAR(512) NOT NULL,
  BAM_INCOMING_BANDWIDTH INT(20) NOT NULL,
  BAM_OUTGOING_BANDWIDTH INT(20) NOT NULL,
  PRIMARY KEY (BAM_ID)
);
  
CREATE TABLE IF NOT EXISTS BAM_BANDWIDTH_STAT_QTR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_QTR_ID INT(10) UNSIGNED NOT NULL,
  BAM_BANDWIDTH_NAME VARCHAR(512) NOT NULL,
  BAM_INCOMING_BANDWIDTH INT(20) NOT NULL,
  BAM_OUTGOING_BANDWIDTH INT(20) NOT NULL,
  PRIMARY KEY (BAM_ID)
);
 
CREATE TABLE IF NOT EXISTS BAM_BANDWIDTH_STAT_YEAR_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_SERVER_ID INT(10) UNSIGNED NOT NULL,
  BAM_YEAR_ID INT(10) UNSIGNED NOT NULL,
  BAM_BANDWIDTH_NAME VARCHAR(512) NOT NULL,
  BAM_INCOMING_BANDWIDTH INT(20) NOT NULL,
  BAM_OUTGOING_BANDWIDTH INT(20) NOT NULL,
  PRIMARY KEY (BAM_ID)
);
  
CREATE TABLE IF NOT EXISTS BAM_REG_BANDWIDTH_USAGE_DAY_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_TENANT_ID INT(10) UNSIGNED NOT NULL,
  BAM_DAY_ID INT(10) UNSIGNED NOT NULL,
  BAM_BANDWIDTH_NAME VARCHAR(512) NOT NULL,
  BAM_REG_BANDWIDTH INT(20) NOT NULL,
  BAM_REG_HISTORY_BANDWIDTH INT(20) NOT NULL,
  PRIMARY KEY (BAM_ID)
);
 
CREATE TABLE IF NOT EXISTS BAM_REG_BANDWIDTH_USAGE_MONTH_FACT (
  BAM_ID INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  BAM_TENANT_ID INT(10) UNSIGNED NOT NULL,
  BAM_MONTH_ID INT(10) UNSIGNED NOT NULL,
  BAM_BANDWIDTH_NAME VARCHAR(512) NOT NULL,
  BAM_REG_BANDWIDTH INT(20) NOT NULL,
  BAM_REG_HISTORY_BANDWIDTH INT(20) NOT NULL,
  PRIMARY KEY (BAM_ID)
);

--
-- CONSTRAINTS 
--

ALTER TABLE BAM_DAY_DIM
  ADD CONSTRAINT BAM_DAY_DIM_IBFK_1 FOREIGN KEY (BAM_MONTH) REFERENCES BAM_MONTH_DIM (BAM_ID);

ALTER TABLE BAM_ENDPOINT_STAT_DAY_FACT
  ADD CONSTRAINT BAM_ENDPOINT_STAT_DAY_FACT_IBFK_2 FOREIGN KEY (BAM_DAY_ID) REFERENCES BAM_DAY_DIM (BAM_ID);
ALTER TABLE BAM_ENDPOINT_STAT_DAY_FACT
  ADD CONSTRAINT BAM_ENDPOINT_STAT_DAY_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_ENDPOINT_STAT_HOUR_FACT
  ADD CONSTRAINT BAM_ENDPOINT_STAT_HOUR_FACT_IBFK_2 FOREIGN KEY (BAM_HOUR_ID) REFERENCES BAM_HOUR_DIM (BAM_ID);
ALTER TABLE BAM_ENDPOINT_STAT_HOUR_FACT
  ADD CONSTRAINT BAM_ENDPOINT_STAT_HOUR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_ENDPOINT_STAT_MONTH_FACT
  ADD CONSTRAINT BAM_ENDPOINT_STAT_MONTH_FACT_IBFK_2 FOREIGN KEY (BAM_MONTH_ID) REFERENCES BAM_MONTH_DIM (BAM_ID);
ALTER TABLE BAM_ENDPOINT_STAT_MONTH_FACT
  ADD CONSTRAINT BAM_ENDPOINT_STAT_MONTH_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_ENDPOINT_STAT_QTR_FACT
  ADD CONSTRAINT BAM_ENDPOINT_STAT_QTR_FACT_IBFK_2 FOREIGN KEY (BAM_QTR_ID) REFERENCES BAM_QTR_DIM (BAM_ID);
ALTER TABLE BAM_ENDPOINT_STAT_QTR_FACT
  ADD CONSTRAINT BAM_ENDPOINT_STAT_QTR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_ENDPOINT_STAT_YEAR_FACT
  ADD CONSTRAINT BAM_ENDPOINT_STAT_YEAR_FACT_IBFK_2 FOREIGN KEY (BAM_YEAR_ID) REFERENCES BAM_YEAR_DIM (BAM_ID);
ALTER TABLE BAM_ENDPOINT_STAT_YEAR_FACT
  ADD CONSTRAINT BAM_ENDPOINT_STAT_YEAR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_HOUR_DIM
  ADD CONSTRAINT BAM_HOUR_DIM_IBFK_1 FOREIGN KEY (BAM_DAY) REFERENCES BAM_DAY_DIM (BAM_ID);

ALTER TABLE BAM_MESSAGE
  ADD CONSTRAINT FK_BAM_MESSAGE_1 FOREIGN KEY (BAM_OP_ID) REFERENCES BAM_OPERATION (BAM_ID);
ALTER TABLE BAM_MESSAGE
  ADD CONSTRAINT FK_BAM_MESSAGE_2 FOREIGN KEY (BAM_ACTIVITY_ID) REFERENCES BAM_ACTIVITY (BAM_ID);

ALTER TABLE BAM_MESSAGE_USER_DATA
  ADD CONSTRAINT FK_BAM_MESSAGE_USER_DATA_1 FOREIGN KEY (BAM_MESSAGE_ID) REFERENCES BAM_MESSAGE (BAM_ID);

ALTER TABLE BAM_MONTH_DIM
  ADD CONSTRAINT BAM_MONTH_DIM_IBFK_1 FOREIGN KEY (BAM_QTR) REFERENCES BAM_QTR_DIM (BAM_ID);

ALTER TABLE BAM_OPERATION
  ADD CONSTRAINT FK_BAM_OPERATION_1 FOREIGN KEY (BAM_SERVICE_ID) REFERENCES BAM_SERVICE (BAM_ID);

ALTER TABLE BAM_OPERATION_DATA
  ADD CONSTRAINT FK_BAM_OPERATION_DATA_1 FOREIGN KEY (BAM_OPERATION_ID) REFERENCES BAM_OPERATION (BAM_ID);

ALTER TABLE BAM_OPERATION_STAT_DAY_FACT
  ADD CONSTRAINT BAM_OPERATION_STAT_DAY_FACT_IBFK_2 FOREIGN KEY (BAM_DAY_ID) REFERENCES BAM_DAY_DIM (BAM_ID);
ALTER TABLE BAM_OPERATION_STAT_DAY_FACT
  ADD CONSTRAINT BAM_OPERATION_STAT_DAY_FACT_IBFK_1 FOREIGN KEY (BAM_OPERATION_ID) REFERENCES BAM_OPERATION (BAM_ID);

ALTER TABLE BAM_OPERATION_STAT_HOUR_FACT
  ADD CONSTRAINT BAM_OPERATION_STAT_HOUR_FACT_IBFK_2 FOREIGN KEY (BAM_HOUR_ID) REFERENCES BAM_HOUR_DIM (BAM_ID);
ALTER TABLE BAM_OPERATION_STAT_HOUR_FACT
  ADD CONSTRAINT BAM_OPERATION_STAT_HOUR_FACT_IBFK_1 FOREIGN KEY (BAM_OPERATION_ID) REFERENCES BAM_OPERATION (BAM_ID);

ALTER TABLE BAM_OPERATION_STAT_MONTH_FACT
  ADD CONSTRAINT BAM_OPERATION_STAT_MONTH_FACT_IBFK_2 FOREIGN KEY (BAM_MONTH_ID) REFERENCES BAM_MONTH_DIM (BAM_ID);
ALTER TABLE BAM_OPERATION_STAT_MONTH_FACT
  ADD CONSTRAINT BAM_OPERATION_STAT_MONTH_FACT_IBFK_1 FOREIGN KEY (BAM_OPERATION_ID) REFERENCES BAM_OPERATION (BAM_ID);

ALTER TABLE BAM_OPERATION_STAT_QTR_FACT
  ADD CONSTRAINT BAM_OPERATION_STAT_QTR_FACT_IBFK_2 FOREIGN KEY (BAM_QTR_ID) REFERENCES BAM_QTR_DIM (BAM_ID);
ALTER TABLE BAM_OPERATION_STAT_QTR_FACT
  ADD CONSTRAINT BAM_OPERATION_STAT_QTR_FACT_IBFK_1 FOREIGN KEY (BAM_OPERATION_ID) REFERENCES BAM_OPERATION (BAM_ID);

ALTER TABLE BAM_OPERATION_STAT_YEAR_FACT
  ADD CONSTRAINT BAM_OPERATION_STAT_YEAR_FACT_IBFK_2 FOREIGN KEY (BAM_YEAR_ID) REFERENCES BAM_YEAR_DIM (BAM_ID);
ALTER TABLE BAM_OPERATION_STAT_YEAR_FACT
  ADD CONSTRAINT BAM_OPERATION_STAT_YEAR_FACT_IBFK_1 FOREIGN KEY (BAM_OPERATION_ID) REFERENCES BAM_OPERATION (BAM_ID);

ALTER TABLE BAM_OPERATION_USER_DATA
  ADD CONSTRAINT FK_BAM_OPERATION_USER_DATA_1 FOREIGN KEY (BAM_OPERATION_ID) REFERENCES BAM_OPERATION (BAM_ID);

ALTER TABLE BAM_PROXY_STAT_DAY_FACT
  ADD CONSTRAINT BAM_PROXY_STAT_DAY_FACT_IBFK_2 FOREIGN KEY (BAM_DAY_ID) REFERENCES BAM_DAY_DIM (BAM_ID);
ALTER TABLE BAM_PROXY_STAT_DAY_FACT
  ADD CONSTRAINT BAM_PROXY_STAT_DAY_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_PROXY_STAT_HOUR_FACT
  ADD CONSTRAINT BAM_PROXY_STAT_HOUR_FACT_IBFK_2 FOREIGN KEY (BAM_HOUR_ID) REFERENCES BAM_HOUR_DIM (BAM_ID);
ALTER TABLE BAM_PROXY_STAT_HOUR_FACT
  ADD CONSTRAINT BAM_PROXY_STAT_HOUR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_PROXY_STAT_MONTH_FACT
  ADD CONSTRAINT BAM_PROXY_STAT_MONTH_FACT_IBFK_2 FOREIGN KEY (BAM_MONTH_ID) REFERENCES BAM_MONTH_DIM (BAM_ID);
ALTER TABLE BAM_PROXY_STAT_MONTH_FACT
  ADD CONSTRAINT BAM_PROXY_STAT_MONTH_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_PROXY_STAT_QTR_FACT
  ADD CONSTRAINT BAM_PROXY_STAT_QTR_FACT_IBFK_2 FOREIGN KEY (BAM_QTR_ID) REFERENCES BAM_QTR_DIM (BAM_ID);
ALTER TABLE BAM_PROXY_STAT_QTR_FACT
  ADD CONSTRAINT BAM_PROXY_STAT_QTR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_PROXY_STAT_YEAR_FACT
  ADD CONSTRAINT BAM_PROXY_STAT_YEAR_FACT_IBFK_2 FOREIGN KEY (BAM_YEAR_ID) REFERENCES BAM_YEAR_DIM (BAM_ID);
ALTER TABLE BAM_PROXY_STAT_YEAR_FACT
  ADD CONSTRAINT BAM_PROXY_STAT_YEAR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_QTR_DIM
  ADD CONSTRAINT BAM_QTR_DIM_IBFK_1 FOREIGN KEY (BAM_YEAR) REFERENCES BAM_YEAR_DIM (BAM_ID);

ALTER TABLE BAM_SEQUENCE_STAT_DAY_FACT
  ADD CONSTRAINT BAM_SEQUENCE_STAT_DAY_FACT_IBFK_2 FOREIGN KEY (BAM_DAY_ID) REFERENCES BAM_DAY_DIM (BAM_ID);
ALTER TABLE BAM_SEQUENCE_STAT_DAY_FACT
  ADD CONSTRAINT BAM_SEQUENCE_STAT_DAY_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_SEQUENCE_STAT_HOUR_FACT
  ADD CONSTRAINT BAM_SEQUENCE_STAT_HOUR_FACT_IBFK_2 FOREIGN KEY (BAM_HOUR_ID) REFERENCES BAM_HOUR_DIM (BAM_ID);
ALTER TABLE BAM_SEQUENCE_STAT_HOUR_FACT
  ADD CONSTRAINT BAM_SEQUENCE_STAT_HOUR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_SEQUENCE_STAT_MONTH_FACT
  ADD CONSTRAINT BAM_SEQUENCE_STAT_MONTH_FACT_IBFK_2 FOREIGN KEY (BAM_MONTH_ID) REFERENCES BAM_MONTH_DIM (BAM_ID);
ALTER TABLE BAM_SEQUENCE_STAT_MONTH_FACT
  ADD CONSTRAINT BAM_SEQUENCE_STAT_MONTH_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_SEQUENCE_STAT_QTR_FACT
  ADD CONSTRAINT BAM_SEQUENCE_STAT_QTR_FACT_IBFK_2 FOREIGN KEY (BAM_QTR_ID) REFERENCES BAM_QTR_DIM (BAM_ID);
ALTER TABLE BAM_SEQUENCE_STAT_QTR_FACT
  ADD CONSTRAINT BAM_SEQUENCE_STAT_QTR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_SEQUENCE_STAT_YEAR_FACT
  ADD CONSTRAINT BAM_SEQUENCE_STAT_YEAR_FACT_IBFK_2 FOREIGN KEY (BAM_YEAR_ID) REFERENCES BAM_YEAR_DIM (BAM_ID);
ALTER TABLE BAM_SEQUENCE_STAT_YEAR_FACT
  ADD CONSTRAINT BAM_SEQUENCE_STAT_YEAR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_SERVER_DATA
  ADD CONSTRAINT FK_BAM_SERVER_DATA_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_SERVER_LOGIN_DATA
  ADD CONSTRAINT FK_BAM_SERVER1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE BAM_SERVER_STAT_DAY_FACT
  ADD CONSTRAINT BAM_SERVER_STAT_DAY_FACT_IBFK_2 FOREIGN KEY (BAM_DAY_ID) REFERENCES BAM_DAY_DIM (BAM_ID);
ALTER TABLE BAM_SERVER_STAT_DAY_FACT
  ADD CONSTRAINT BAM_SERVER_STAT_DAY_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_SERVER_STAT_HOUR_FACT
  ADD CONSTRAINT BAM_SERVER_STAT_HOUR_FACT_IBFK_2 FOREIGN KEY (BAM_HOUR_ID) REFERENCES BAM_HOUR_DIM (BAM_ID);
ALTER TABLE BAM_SERVER_STAT_HOUR_FACT
  ADD CONSTRAINT BAM_SERVER_STAT_HOUR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_SERVER_STAT_MONTH_FACT
  ADD CONSTRAINT BAM_SERVER_STAT_MONTH_FACT_IBFK_2 FOREIGN KEY (BAM_MONTH_ID) REFERENCES BAM_MONTH_DIM (BAM_ID);
ALTER TABLE BAM_SERVER_STAT_MONTH_FACT
  ADD CONSTRAINT BAM_SERVER_STAT_MONTH_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_SERVER_STAT_QTR_FACT
  ADD CONSTRAINT BAM_SERVER_STAT_QTR_FACT_IBFK_2 FOREIGN KEY (BAM_QTR_ID) REFERENCES BAM_QTR_DIM (BAM_ID);
ALTER TABLE BAM_SERVER_STAT_QTR_FACT
  ADD CONSTRAINT BAM_SERVER_STAT_QTR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_SERVER_STAT_YEAR_FACT
  ADD CONSTRAINT BAM_SERVER_STAT_YEAR_FACT_IBFK_2 FOREIGN KEY (BAM_YEAR_ID) REFERENCES BAM_YEAR_DIM (BAM_ID);
ALTER TABLE BAM_SERVER_STAT_YEAR_FACT
  ADD CONSTRAINT BAM_SERVER_STAT_YEAR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_SERVER_USER_DATA
  ADD CONSTRAINT FK_BAM_SERVER_USER_DATA_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_SERVER_USER_LOGIN_DATA
  ADD CONSTRAINT FK_BAM_SERVER_USER_LOGIN_DATA_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_SERVICE
  ADD CONSTRAINT FK_BAM_SERVICE_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_SERVICE_DATA
  ADD CONSTRAINT FK_BAM_SERVICE_DATA_1 FOREIGN KEY (BAM_SERVICE_ID) REFERENCES BAM_SERVICE (BAM_ID);

ALTER TABLE BAM_SERVICE_STAT_DAY_FACT
  ADD CONSTRAINT BAM_SERVICE_STAT_DAY_FACT_IBFK_2 FOREIGN KEY (BAM_DAY_ID) REFERENCES BAM_DAY_DIM (BAM_ID);
ALTER TABLE BAM_SERVICE_STAT_DAY_FACT
  ADD CONSTRAINT BAM_SERVICE_STAT_DAY_FACT_IBFK_1 FOREIGN KEY (BAM_SERVICE_ID) REFERENCES BAM_SERVICE (BAM_ID);

ALTER TABLE BAM_SERVICE_STAT_HOUR_FACT
  ADD CONSTRAINT BAM_SERVICE_STAT_HOUR_FACT_IBFK_2 FOREIGN KEY (BAM_HOUR_ID) REFERENCES BAM_HOUR_DIM (BAM_ID);
ALTER TABLE BAM_SERVICE_STAT_HOUR_FACT
  ADD CONSTRAINT BAM_SERVICE_STAT_HOUR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVICE_ID) REFERENCES BAM_SERVICE (BAM_ID);

ALTER TABLE BAM_SERVICE_STAT_MONTH_FACT
  ADD CONSTRAINT BAM_SERVICE_STAT_MONTH_FACT_IBFK_2 FOREIGN KEY (BAM_MONTH_ID) REFERENCES BAM_MONTH_DIM (BAM_ID);
ALTER TABLE BAM_SERVICE_STAT_MONTH_FACT
  ADD CONSTRAINT BAM_SERVICE_STAT_MONTH_FACT_IBFK_1 FOREIGN KEY (BAM_SERVICE_ID) REFERENCES BAM_SERVICE (BAM_ID);

ALTER TABLE BAM_SERVICE_STAT_QTR_FACT
  ADD CONSTRAINT BAM_SERVICE_STAT_QTR_FACT_IBFK_2 FOREIGN KEY (BAM_QTR_ID) REFERENCES BAM_QTR_DIM (BAM_ID);
ALTER TABLE BAM_SERVICE_STAT_QTR_FACT
  ADD CONSTRAINT BAM_SERVICE_STAT_QTR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVICE_ID) REFERENCES BAM_SERVICE (BAM_ID);

ALTER TABLE BAM_SERVICE_STAT_YEAR_FACT
  ADD CONSTRAINT BAM_SERVICE_STAT_YEAR_FACT_IBFK_2 FOREIGN KEY (BAM_YEAR_ID) REFERENCES BAM_YEAR_DIM (BAM_ID);
ALTER TABLE BAM_SERVICE_STAT_YEAR_FACT
  ADD CONSTRAINT BAM_SERVICE_STAT_YEAR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVICE_ID) REFERENCES BAM_SERVICE (BAM_ID);

ALTER TABLE BAM_SERVICE_USER_DATA
  ADD CONSTRAINT FK_BAM_SERVICE_USER_DATA_1 FOREIGN KEY (BAM_SERVICE_ID) REFERENCES BAM_SERVICE (BAM_ID);
  
ALTER TABLE BAM_BANDWIDTH_STAT_HOUR_FACT
  ADD CONSTRAINT BAM_BANDWIDTH_STAT_HOUR_FACT_IBFK_2 FOREIGN KEY (BAM_HOUR_ID) REFERENCES BAM_HOUR_DIM (BAM_ID);
ALTER TABLE BAM_BANDWIDTH_STAT_HOUR_FACT
  ADD CONSTRAINT BAM_BANDWIDTH_STAT_HOUR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);

ALTER TABLE BAM_BANDWIDTH_STAT_DAY_FACT
  ADD CONSTRAINT BAM_BANDWIDTH_STAT_DAY_FACT_IBFK_2 FOREIGN KEY (BAM_DAY_ID) REFERENCES BAM_DAY_DIM (BAM_ID);
ALTER TABLE BAM_BANDWIDTH_STAT_DAY_FACT
  ADD CONSTRAINT BAM_BANDWIDTH_STAT_DAY_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);
 
ALTER TABLE BAM_BANDWIDTH_STAT_MONTH_FACT
  ADD CONSTRAINT BAM_BANDWIDTH_STAT_MONTH_FACT_IBFK_2 FOREIGN KEY (BAM_MONTH_ID) REFERENCES BAM_MONTH_DIM (BAM_ID);
ALTER TABLE BAM_BANDWIDTH_STAT_MONTH_FACT
  ADD CONSTRAINT BAM_BANDWIDTH_STAT_MONTH_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);
 
ALTER TABLE BAM_BANDWIDTH_STAT_QTR_FACT
  ADD CONSTRAINT BAM_BANDWIDTH_STAT_QTR_FACT_IBFK_2 FOREIGN KEY (BAM_QTR_ID) REFERENCES BAM_QTR_DIM (BAM_ID);
ALTER TABLE BAM_BANDWIDTH_STAT_QTR_FACT
  ADD CONSTRAINT BAM_BANDWIDTH_STAT_QTR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);  
 
ALTER TABLE BAM_BANDWIDTH_STAT_YEAR_FACT
  ADD CONSTRAINT BAM_BANDWIDTH_STAT_YEAR_FACT_IBFK_2 FOREIGN KEY (BAM_YEAR_ID) REFERENCES BAM_YEAR_DIM (BAM_ID);
ALTER TABLE BAM_BANDWIDTH_STAT_YEAR_FACT
  ADD CONSTRAINT BAM_BANDWIDTH_STAT_YEAR_FACT_IBFK_1 FOREIGN KEY (BAM_SERVER_ID) REFERENCES BAM_SERVER (BAM_SERVER_ID);  
  
ALTER TABLE BAM_REG_BANDWIDTH_USAGE_DAY_FACT
  ADD CONSTRAINT BAM_REG_BANDWIDTH_USAGE_DAY_FACT_IBFK_1 FOREIGN KEY (BAM_DAY_ID) REFERENCES BAM_DAY_DIM (BAM_ID);  
  
ALTER TABLE BAM_REG_BANDWIDTH_USAGE_MONTH_FACT
  ADD CONSTRAINT BAM_REG_BANDWIDTH_USAGE_MONTH_FACT_IBFK_1 FOREIGN KEY (BAM_MONTH_ID) REFERENCES BAM_MONTH_DIM (BAM_ID);



