CREATE TABLE IDN_BASE_TABLE (
            PRODUCT_NAME VARCHAR2 (20),
            PRIMARY KEY (PRODUCT_NAME)
)
/

INSERT INTO IDN_BASE_TABLE values ('WSO2 Identity Server');

CREATE TABLE IDN_OAUTH_CONSUMER_APPS (
            CONSUMER_KEY VARCHAR2 (512),
            CONSUMER_SECRET VARCHAR2 (512),
            USERNAME VARCHAR2 (255),
            TENANT_ID INTEGER DEFAULT 0,
            APP_NAME VARCHAR2 (255),
            OAUTH_VERSION VARCHAR2 (128),
            CALLBACK_URL VARCHAR2 (1024),
            PRIMARY KEY (CONSUMER_KEY)
)
/

CREATE TABLE IDN_OAUTH1A_REQUEST_TOKEN (
            REQUEST_TOKEN VARCHAR2 (512),
            REQUEST_TOKEN_SECRET VARCHAR2 (512),
            CONSUMER_KEY VARCHAR2 (512),
            CALLBACK_URL VARCHAR2 (1024),
            SCOPE VARCHAR2(2048),
            AUTHORIZED VARCHAR2 (128),
            OAUTH_VERIFIER VARCHAR2 (512),
            AUTHZ_USER VARCHAR2 (512),
            PRIMARY KEY (REQUEST_TOKEN),
            FOREIGN KEY (CONSUMER_KEY) REFERENCES IDN_OAUTH_CONSUMER_APPS(CONSUMER_KEY)
)
/


CREATE TABLE IDN_OAUTH1A_ACCESS_TOKEN (
            ACCESS_TOKEN VARCHAR2 (512),
            ACCESS_TOKEN_SECRET VARCHAR2 (512),
            CONSUMER_KEY VARCHAR2 (512),
            SCOPE VARCHAR2(2048),
            AUTHZ_USER VARCHAR2 (512),
            PRIMARY KEY (ACCESS_TOKEN),
            FOREIGN KEY (CONSUMER_KEY) REFERENCES IDN_OAUTH_CONSUMER_APPS(CONSUMER_KEY)
)
/

CREATE TABLE IDN_OAUTH2_AUTHORIZATION_CODE (
            AUTHORIZATION_CODE VARCHAR2 (512),
            CONSUMER_KEY VARCHAR2 (512),
            SCOPE VARCHAR2(2048),
            AUTHZ_USER VARCHAR2 (512),
            TIME_CREATED TIMESTAMP,
            VALIDITY_PERIOD NUMBER(19),
            PRIMARY KEY (AUTHORIZATION_CODE),
            FOREIGN KEY (CONSUMER_KEY) REFERENCES IDN_OAUTH_CONSUMER_APPS(CONSUMER_KEY)
)
/

CREATE TABLE IDN_OAUTH2_ACCESS_TOKEN (
			ACCESS_TOKEN VARCHAR2 (255),
			REFRESH_TOKEN VARCHAR2 (255),
			CONSUMER_KEY VARCHAR2 (255),
			AUTHZ_USER VARCHAR2 (255),
			USER_TYPE VARCHAR2 (255),
			TIME_CREATED TIMESTAMP,
			VALIDITY_PERIOD NUMBER(19),
			TOKEN_SCOPE VARCHAR2 (512),
			TOKEN_STATE VARCHAR2 (128) DEFAULT 'ACTIVE',
			PRIMARY KEY (ACCESS_TOKEN),
            FOREIGN KEY (CONSUMER_KEY) REFERENCES IDN_OAUTH_CONSUMER_APPS(CONSUMER_KEY)
)
/


CREATE TABLE AM_SUBSCRIBER (
    SUBSCRIBER_ID INTEGER,
    USER_ID VARCHAR2(50) NOT NULL,
    TENANT_ID INTEGER NOT NULL,
    EMAIL_ADDRESS VARCHAR2(256) NULL,
    DATE_SUBSCRIBED DATE NOT NULL,
    PRIMARY KEY (SUBSCRIBER_ID),
    UNIQUE (TENANT_ID,USER_ID)
)
/

CREATE SEQUENCE AM_SUBSCRIBER_SEQUENCE START WITH 1 INCREMENT BY 1
/

CREATE OR REPLACE TRIGGER AM_SUBSCRIBER_TRIGGER
		            BEFORE INSERT
                    ON AM_SUBSCRIBER
                    REFERENCING NEW AS NEW
                    FOR EACH ROW
                    BEGIN
                    SELECT AM_SUBSCRIBER_SEQUENCE.nextval INTO :NEW.SUBSCRIBER_ID FROM dual;
                    END;
/
--TODO: Have to add ON UPDATE CASCADE for the FOREIGN KEY(SUBSCRIBER_ID) relation
CREATE TABLE AM_APPLICATION (
    APPLICATION_ID INTEGER,
    NAME VARCHAR2(100),
    SUBSCRIBER_ID INTEGER,
    APPLICATION_TIER VARCHAR2(50) DEFAULT 'Unlimited',
    CALLBACK_URL VARCHAR(512),
    FOREIGN KEY(SUBSCRIBER_ID) REFERENCES AM_SUBSCRIBER(SUBSCRIBER_ID) ON DELETE CASCADE,
    PRIMARY KEY(APPLICATION_ID),
    UNIQUE (NAME,SUBSCRIBER_ID)
)
/

CREATE SEQUENCE AM_APPLICATION_SEQUENCE START WITH 1 INCREMENT BY 1
/

CREATE OR REPLACE TRIGGER AM_APPLICATION_TRIGGER
		            BEFORE INSERT
                    ON AM_APPLICATION
                    REFERENCING NEW AS NEW
                    FOR EACH ROW
                    BEGIN
                    SELECT AM_APPLICATION_SEQUENCE.nextval INTO :NEW.APPLICATION_ID FROM dual;
                    END;
/

CREATE TABLE AM_API (
    API_ID INTEGER,
    API_PROVIDER VARCHAR2(256),
    API_NAME VARCHAR2(256),
    API_VERSION VARCHAR2(30),
    CONTEXT VARCHAR2(256),
    PRIMARY KEY(API_ID),
    UNIQUE (API_PROVIDER,API_NAME,API_VERSION)
)
/

CREATE SEQUENCE AM_API_SEQUENCE START WITH 1 INCREMENT BY 1
/

CREATE OR REPLACE TRIGGER AM_API_TRIGGER
		            BEFORE INSERT
                    ON AM_API
                    REFERENCING NEW AS NEW
                    FOR EACH ROW
                    BEGIN
                    SELECT AM_API_SEQUENCE.nextval INTO :NEW.API_ID FROM dual;
                    END;
/
CREATE TABLE AM_API_URL_MAPPING (
    URL_MAPPING_ID INTEGER,
    API_ID INTEGER NOT NULL,
    HTTP_METHOD VARCHAR(20) NULL,
    AUTH_SCHEME VARCHAR(50) NULL,
    URL_PATTERN VARCHAR(512) NULL,
    PRIMARY KEY(URL_MAPPING_ID)
)
/

CREATE SEQUENCE AM_API_URL_MAPPING_SEQUENCE START WITH 1 INCREMENT BY 1
/
--TODO: Have to add ON UPDATE CASCADE for the FOREIGN KEY(SUBSCRIPTION_ID) relation
CREATE TABLE AM_SUBSCRIPTION (
    SUBSCRIPTION_ID INTEGER,
    TIER_ID VARCHAR2(50),
    API_ID INTEGER,
    LAST_ACCESSED DATE NULL,
    APPLICATION_ID INTEGER,
    FOREIGN KEY(APPLICATION_ID) REFERENCES AM_APPLICATION(APPLICATION_ID) ON DELETE CASCADE,
    FOREIGN KEY(API_ID) REFERENCES AM_API(API_ID) ON DELETE CASCADE,
    PRIMARY KEY (SUBSCRIPTION_ID)
)
/

CREATE SEQUENCE AM_SUBSCRIPTION_SEQUENCE START WITH 1 INCREMENT BY 1
/

CREATE OR REPLACE TRIGGER AM_SUBSCRIPTION_TRIGGER
		            BEFORE INSERT
                    ON AM_SUBSCRIPTION
                    REFERENCING NEW AS NEW
                    FOR EACH ROW
                    BEGIN
                    SELECT AM_SUBSCRIPTION_SEQUENCE.nextval INTO :NEW.SUBSCRIPTION_ID FROM dual;
                    END;
/
--TODO: Have to add ON UPDATE CASCADE for the FOREIGN KEY(APPLICATION_ID) and FOREIGN KEY(API_ID) relations
CREATE TABLE AM_SUBSCRIPTION_KEY_MAPPING (
    SUBSCRIPTION_ID INTEGER,
    ACCESS_TOKEN VARCHAR2(512),
    KEY_TYPE VARCHAR2(512) NOT NULL,
    FOREIGN KEY(SUBSCRIPTION_ID) REFERENCES AM_SUBSCRIPTION(SUBSCRIPTION_ID) ON DELETE CASCADE,
    PRIMARY KEY(SUBSCRIPTION_ID,ACCESS_TOKEN)
)
/
--TODO: Have to add ON UPDATE CASCADE for the FOREIGN KEY(APPLICATION_ID) relation
CREATE TABLE AM_APPLICATION_KEY_MAPPING (
    APPLICATION_ID INTEGER,
    CONSUMER_KEY VARCHAR2(512),
    KEY_TYPE VARCHAR2(512) NOT NULL,
    FOREIGN KEY(APPLICATION_ID) REFERENCES AM_APPLICATION(APPLICATION_ID) ON DELETE CASCADE,
    PRIMARY KEY(APPLICATION_ID,CONSUMER_KEY)
)
/
--TODO: Have to add ON UPDATE CASCADE for the FOREIGN KEY(API_ID) relation
CREATE TABLE AM_API_LC_EVENT (
    EVENT_ID INTEGER,
    API_ID INTEGER NOT NULL,
    PREVIOUS_STATE VARCHAR2(50),
    NEW_STATE VARCHAR2(50) NOT NULL,
    USER_ID VARCHAR2(50) NOT NULL,
    TENANT_ID INTEGER NOT NULL,
    EVENT_DATE DATE NOT NULL,
    FOREIGN KEY(API_ID) REFERENCES AM_API(API_ID) ON DELETE CASCADE,
    PRIMARY KEY (EVENT_ID)
)
/

CREATE SEQUENCE AM_API_LC_EVENT_SEQUENCE START WITH 1 INCREMENT BY 1
/

CREATE OR REPLACE TRIGGER AM_API_LC_EVENT_TRIGGER
		            BEFORE INSERT
                    ON AM_API_LC_EVENT
                    REFERENCING NEW AS NEW
                    FOR EACH ROW
                    BEGIN
                    SELECT AM_API_LC_EVENT_SEQUENCE.nextval INTO :NEW.EVENT_ID FROM dual;
                    END;
/

CREATE INDEX IDX_SUB_APP_ID ON AM_SUBSCRIPTION (APPLICATION_ID, SUBSCRIPTION_ID)
/
