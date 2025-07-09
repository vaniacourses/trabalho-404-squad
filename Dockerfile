# Use OpenJDK 8 as base image to match the Java version in pom.xml
FROM openjdk:8-jdk-alpine

# Add maintainer info
LABEL maintainer="originmobi.net"

# Add a volume pointing to /tmp
VOLUME /tmp

# Make port 8080 available to the world outside this container
EXPOSE 8080

# The application's jar file (change the name if needed)
ARG JAR_FILE=target/pdv-0.0.1-SNAPSHOT.war

# Add the application's jar to the container
ADD ${JAR_FILE} app.war

# Run the jar file
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.war"]