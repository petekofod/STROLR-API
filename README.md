# strolr2
STROLR2 - Self Service Tool for Railwaynet Onboard Log Retrieval version 2.0

Use Java 1.8.0_242 from
https://github.com/AdoptOpenJDK/openjdk8-upstream-binaries/releases/tag/jdk8u242-b08

## Deployment

We have a staging environment at strolr3.railwaynet.datasages.com. 
This is Amazon Linux. 
Java is installed using:

`sudo yum install java-1.8.0-openjdk`

The application is located at `/home/ec2-user/strolr`

In order to deploy the application use the following steps:
* run `build.sh` in your development environment
* review and edit `build/application.properties`
* update `deploy.sh` with the name of your ssh key for the staging server
* run `deploy.sh` , it basically `scp` everything from "build" directory to the staging server
* ssh to the staging server and run `start.sh` from `/home/ec2-user/strolr`

The application directory should contain:
* the jar file of the application
* the application.properties configuration file
* start.sh file
* the keystore