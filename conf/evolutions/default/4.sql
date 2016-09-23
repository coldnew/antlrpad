# --- !Ups

ALTER TABLE "ParsedResults"
ADD ("code" varchar);

# --- !Downs

ALTER TABLE "ParsedResults"
DROP COLUMN code;