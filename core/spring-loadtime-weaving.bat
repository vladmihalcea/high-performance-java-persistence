@echo off

call mvn -Dcmd.args="-javaagent:%M2_REPOSITORY%\org\springframework\spring-instrument\6.0.8\spring-instrument-6.0.8.jar" -Dtest=SpringDataJPARuntimeBytecodeEnhancementTest test

goto:eof