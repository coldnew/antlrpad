# --- !Ups

ALTER TABLE "ParsedResults"
ADD ("code" varchar);

CREATE INDEX CodeIdx
ON "ParsedResults" ("code")

# --- !Downs

ALTER TABLE "ParsedResults"
DROP COLUMN code;