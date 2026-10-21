FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/student-placement-portal-1.0.0.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-Dserver.port=8085", "-jar", "/app/app.jar"]
