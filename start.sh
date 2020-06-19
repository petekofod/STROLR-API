
java \
	-classpath "/$PWD/src/main/resources" \
	-Dspring.profiles.active=production \
	-Dkeystore.file=file:///$PWD/src/main/resources/logdelivery.railwaynet.datasages.com.new.p12 -Dkeystore.pass=password##123 \
	-Dtruststore.file=file:///$PWD/src/main/resources/RailwaynetEnvARootCA-chain.jks -Dtruststore.pass=changeit \
	-Dserver.port=8443 \
	-jar target/strolr2-0.0.2-SNAPSHOT.jar
