CREATE SCHEMA IF NOT EXISTS storage_service;

SET SCHEMA storage_service;

CREATE TABLE IF NOT EXISTS bucket
(
    id          bigint       NOT NULL AUTO_INCREMENT,
    code        varchar(512) NOT NULL UNIQUE,
    created_at  timestamp    NOT NULL,
    modified_at timestamp    NOT NULL,
    readonly    boolean      NOT NULL,
    CONSTRAINT bucket_pk PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS content
(
    uid         varchar(255) NOT NULL,
    name        varchar(255) NOT NULL,
    bucket_id   INTEGER      NOT NULL,
    created_at  timestamp    NOT NULL,
    modified_at timestamp    NOT NULL,
    FOREIGN KEY (bucket_id) REFERENCES bucket (id),
    CONSTRAINT content_pk PRIMARY KEY (uid)
);

CREATE TABLE IF NOT EXISTS content_meta
(
    id          bigint        NOT NULL AUTO_INCREMENT,
    meta_key    varchar(255)  NOT NULL,
    meta_value  varchar(2048) NOT NULL,
    content_uid varchar(255)  NOT NULL,
    FOREIGN KEY (content_uid) REFERENCES content (uid),
    CONSTRAINT content_meta_pk PRIMARY KEY (id)
);
