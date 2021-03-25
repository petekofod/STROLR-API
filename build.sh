#!/bin/bash

echo Build STROLR

rm build/*

mvn clean install

cp target/*.jar build/
cp src/main/resources/start.sh build/

