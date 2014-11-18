#!/bin/sh
java -cp ~/apps/src/selenium-grid-setup/grid-test/target/grid-test-0.0.1-SNAPSHOT.jar \
org.testng.TestNG \
~/apps/src/selenium-grid-setup/grid-test/src/main/resources/testng-grid.xml 

