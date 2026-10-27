-- schema fixo do JdbcUserDetailsManager (Spring Security)
create table if not exists users(username varchar(50) not null primary key,password varchar(500) not null, enabled boolean not null);
create table if not exists authorities (username varchar(50) not null,authority varchar_ignorecase(50) not null,constraint fk_authorities_users foreign key(username) references users(username));
create unique index if not exists ix_auth_username on authorities (username,authority);

-- tabelas do enunciado
-- correcoes: removida a virgula final apos updated_at; senha ampliada de VARCHAR(10) para VARCHAR(255) (hash BCrypt)
CREATE TABLE IF NOT EXISTS login_attempts (
    username VARCHAR(100) PRIMARY KEY,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    last_failed_at TIMESTAMP NULL,
    locked_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS usuarios (
    username VARCHAR(100) PRIMARY KEY,
    senha VARCHAR(255)
);
