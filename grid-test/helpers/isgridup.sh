#!/bin/sh
java -cp ~/apps/src/selenium-grid-setup/grid-test/target/grid-setup-test-1.0.jar \
org.testng.TestNG \
~/apps/src/selenium-grid-setup/grid-test/src/main/resources/testng-grid.xml 

