# Imagen del backend, la que construye Railway.
#
# Se define a mano y no se deja que Railway lo adivine: sin esto elige la
# version de Java por su cuenta, y este proyecto necesita 21.
#
# Dos etapas: la primera compila con el JDK y Maven, la segunda se queda solo
# con el jar y un JRE. Asi la imagen final no lleva el compilador ni el
# repositorio de dependencias, que pesan varios cientos de MB.

FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /app

# El wrapper y el pom primero, y las dependencias despues: mientras el pom no
# cambie, Docker reutiliza esa capa y no vuelve a bajar todo en cada deploy.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src ./src
# Los tests necesitan una base PostgreSQL, que no existe durante el build.
# Corren en la maquina de cada uno antes de pushear.
RUN ./mvnw -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilacion /app/target/*.jar app.jar

# Sin esto la JVM asume que tiene toda la RAM de la maquina y el contenedor la
# mata cuando se pasa del limite del plan.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# Railway ignora este valor y publica el puerto que indica la variable PORT;
# sirve para correr la imagen a mano.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
