# --- !Ups

ALTER TABLE "ParsedResults"
ADD ("lexer" varchar);

# --- !Downs

ALTER TABLE "ParsedResults"
DROP COLUMN lexer;