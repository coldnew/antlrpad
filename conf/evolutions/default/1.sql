# --- !Ups
CREATE TABLE "ParsedResults"("id" SERIAL PRIMARY KEY ,"grammar" varchar , "src" varchar, "tree" varchar);

# --- !Downs

DROP TABLE "employee";