FROM openjdk:8-alpine

COPY target/uberjar/mtrack.jar /mtrack/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/mtrack/app.jar"]
