# --- !Ups

ALTER TABLE "ParsedResults"
ADD ("code" varchar);

CREATE UNIQUE INDEX CodeIdx
ON "ParsedResults" ("code")

# --- !Downs

ALTER TABLE "ParsedResults"
DROP COLUMN code;