#!/bin/bash

rm build/*

mvn clean install

cp target/*.jar build/
cp src/main/resources/start.sh build/
cp src/main/resources/application.properties build/
cp src/main/resources/logdelivery.railwaynet.datasages.com.new.p12 build/

