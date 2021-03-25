#!/bin/bash

echo Build STROLR API

rm build/*

mvn clean install

cp target/*.jar build/
cp src/main/resources/start.sh build/

