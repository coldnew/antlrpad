# --- !Ups

ALTER TABLE "ParsedResults"
ADD ("rules" varchar, "rule" varchar);

# --- !Downs

ALTER TABLE "ParsedResults"
DROP COLUMN rules, rule;