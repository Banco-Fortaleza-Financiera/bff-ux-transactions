# bff-ux-transactions

Proyecto Java 21 con Gradle Groovy, Spring Boot 3.x, Spring Cloud OpenFeign, OpenAPI Generator, pruebas con JUnit 5 y JaCoCo.

## Requisitos

- Java 21
- Gradle Wrapper incluido en el proyecto
- Git para instalar el hook de pre-commit

## Como ejecutar

```sh
./gradlew bootRun
```

La aplicacion inicia en `http://localhost:8080`.

## Como correr pruebas

```sh
./gradlew test
```

Para ejecutar el build completo:

```sh
./gradlew clean build
```

## Regenerar OpenAPI

Las especificaciones viven en:

```text
src/main/resources/openapi.yaml
src/main/resources/openapi-client.yaml
src/main/resources/open-api-token-validation.yaml
```

Regenerar codigo y compilar:

```sh
./gradlew clean compileJava
```

El codigo generado queda en `build/generated/src/main/java` y se agrega al `sourceSets.main`.

## Arquitectura

La aplicacion conserva la estructura base del BFF UX de cuentas, preparada para implementar transacciones:

- `domain`: modelos y excepciones de dominio sin dependencias de Spring.
- `controller`: controladores REST que implementan las interfaces generadas.
- `services`: casos de uso y coordinacion hacia servicios de soporte.
- `handler`: manejo transversal de errores HTTP.
- `configuration`: configuracion Spring, logging e interceptores.
- `utils`: logging y utilidades compartidas.

El esqueleto incluye la funcionalidad minima para validar token, resolver el usuario autenticado y delegar operaciones al SP de transacciones.

## Docker

Construir imagen:

```sh
docker build -t bff-ux-transactions .
```

Ejecutar contenedor:

```sh
docker run --rm -p 8080:8080 bff-ux-transactions
```
