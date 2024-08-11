LABEL authors="pablomiranda"

#JAVA 17
FROM openjdk:17-jdk-alpine

#Working dir for the container
WORKDIR /app

#Copies JAR file from the machine to the container
COPY target/tourguide-0.0.1-SNAPSHOT.jar /app/tourguide.jar

# Port to expose the app
EXPOSE 8080

#Command to run the application
ENTRYPOINT ["java", "-jar", "tourguide.jar"]
