# ---- Build Stage ----
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copiar pom y descargar dependencias
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Copiar el resto del proyecto
COPY src ./src

# Compilar
RUN mvn -q clean package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copiar el JAR generado
COPY --from=build /app/target/*.jar app.jar

# Puerto interno
EXPOSE 8080

# API externa definida como variable de entorno
ENV EXTERNAL_API_BOOKS_URL="https://my-json-server.typicode.com/Gabriel-Arriola-UTN/libros/books"

# Pasar la variable al Spring Boot
ENV JAVA_OPTS="-Dexternal.api.books.url=$EXTERNAL_API_BOOKS_URL"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
