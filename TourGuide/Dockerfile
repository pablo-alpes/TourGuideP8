FROM openjdk:22-jdk-slim

#Working dir for the container
WORKDIR /app

#Copies JAR file from the machine to the container
COPY target/tourguide-0.0.1-SNAPSHOT.jar /app/tourguide-0.0.1-SNAPSHOT.jar
COPY libs/ /app/libs/

# Port to expose all the microservices
EXPOSE 8080 8081 8082 8083

#Command to run the application/ to be overriden by the docker compose
ENTRYPOINT ["java", "-jar", "tourguide-0.0.1-SNAPSHOT.jar"]
