@echo off

pushd core
call mvn -D skipTests clean install
popd

pushd jooq
call mvn -D skipTests clean install
call mvn test-compile
popd

goto:eof