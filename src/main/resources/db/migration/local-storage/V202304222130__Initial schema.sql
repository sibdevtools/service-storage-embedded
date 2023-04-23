CREATE TABLE IF NOT EXISTS bucket
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    code       varchar(16) NOT NULL UNIQUE,
    created_at timestamp   NOT NULL,
    readonly   boolean     NOT NULL
);

CREATE TABLE IF NOT EXISTS content
(
    uid         varchar(255) PRIMARY KEY,
    name        varchar(255) NOT NULL,
    bucket_id   INTEGER      NOT NULL,
    created_at  timestamp    NOT NULL,
    modified_at timestamp    NOT NULL,
    foreign key (bucket_id) references bucket (id)
);

CREATE TABLE IF NOT EXISTS content_meta
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    meta_key    varchar(255)  NOT NULL,
    meta_value  varchar(2048) NOT NULL,
    content_uid varchar(255)  NOT NULL,
    foreign key (content_uid) references content (uid)
);
