@echo off
REM 创建输出目录
mkdir out\production\lab
mkdir out\test\lab

REM 编译源代码
javac -d out/production/lab TextGraph.java Main.java

REM 编译测试代码
javac -cp "out/production/lab;lib/junit-4.13.2.jar;lib/hamcrest-core-1.3.jar" -d out/test/lab TextGraphTest.java

REM 运行测试并生成覆盖率数据
java -javaagent:lib/jacocoagent.jar=destfile=jacoco.exec -cp "out/production/lab;out/test/lab;lib/junit-4.13.2.jar;lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore TextGraphTest

REM 生成覆盖率报告
java -jar lib/jacococli.jar report jacoco.exec --classfiles out/production/lab --sourcefiles . --html report
