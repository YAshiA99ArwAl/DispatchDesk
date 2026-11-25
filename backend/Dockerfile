FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:copy-dependencies -DoutputDirectory=target/lib
COPY src ./src
RUN mvn -q compile

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/classes ./classes
COPY --from=build /app/target/lib ./lib
COPY dispatchdesk.db ./dispatchdesk.db
EXPOSE 8080
ENV PORT=8080
CMD ["java", "-cp", "classes:lib/*", "com.dispatchdesk.BackendMain"]
