# --- !Ups

CREATE TABLE "ParsedResults"(
    "id" SERIAL PRIMARY KEY,
    "code"      varchar,
    "grammar"   varchar,
    "lexer"     varchar,
    "src"       varchar,
    "rule"      varchar);

CREATE UNIQUE INDEX CodeIdx ON "ParsedResults" ("code");

# --- !Downs

DROP TABLE "ParsedResults";