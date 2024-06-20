@echo off

pushd core
call mvn clean test-compile
popd

pushd jooq
call mvn clean 
call mvn install jooq-core 
call mvn test-compile

goto:eof