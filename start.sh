
java -Dspring.profiles.active=production \
	-Dkeystore.file=file:///$PWD/src/main/resources/keystore.p12 -Dkeystore.pass=strolr \
	-Dtruststore.file=file:///$PWD/src/main/resources/truststore.jks -Dtruststore.pass=strolr \
	-Dserver.port=8443 \
	-jar target/strolr2-0.0.1-SNAPSHOT.jar
