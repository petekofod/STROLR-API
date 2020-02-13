java -Dspring.profiles.active=production ^
	-Dkeystore.file=file:///%~dp0/src/main/resources/keystore.p12 -Dkeystore.pass=strolr ^
	-Dtruststore.file=file:///%~dp0/src/main/resources/truststore.jks -Dtruststore.pass=strolr ^
	-Dserver.port=8447 ^
	-jar target\strolr2-0.0.1-SNAPSHOT.jar 
