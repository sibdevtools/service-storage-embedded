CREATE TABLE IF NOT EXISTS STORAGE_BUCKET
(
    id         bigint auto_increment PRIMARY KEY,
    code       varchar(16) NOT NULL UNIQUE,
    created_at timestamp   NOT NULL,
    readonly   boolean     NOT NULL
);

CREATE TABLE IF NOT EXISTS STORAGE_CONTENT
(
    uid         varchar(255) PRIMARY KEY,
    name        varchar(255) NOT NULL,
    bucket_id   bigint       NOT NULL,
    created_at  timestamp    NOT NULL,
    modified_at timestamp    NOT NULL,
    foreign key (bucket_id) references STORAGE_BUCKET (id)
);

CREATE TABLE IF NOT EXISTS STORAGE_CONTENT_META
(
    id         bigint auto_increment PRIMARY KEY,
    "key"      varchar(255)  NOT NULL,
    "value"    varchar(2048) NOT NULL,
    content_id varchar(255)  NOT NULL,
    foreign key (content_id) references STORAGE_CONTENT (uid)
);
