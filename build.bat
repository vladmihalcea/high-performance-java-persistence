@echo off

pushd core
call mvn clean test-compile
popd

pushd jooq
call mvn clean
call mvn test-compile
popd

goto:eof