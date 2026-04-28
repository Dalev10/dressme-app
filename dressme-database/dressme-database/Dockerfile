# Usamos una imagen base ligera de Eclipse Temurin para Java 21
FROM eclipse-temurin:21-jdk-alpine
VOLUME /tmp
# Argumento para apuntar al .jar generado por Maven
ARG JAR_FILE=target/*.jar
# Copiamos el .jar al contenedor
COPY ${JAR_FILE} app.jar
# Exponemos el puerto interno
EXPOSE 8080
# Comando para ejecutar la aplicación
ENTRYPOINT ["java","-jar","/app.jar"]