# Grenlus Signage

Sistema de cartelería digital centralizada. Desde un panel web en la oficina se
decide qué contenido reproduce cada pantalla, aunque esté en otra sucursal y
otra red.

> **Regla de arquitectura:** la oficina define el *estado deseado*, el backend lo
> persiste, y cada player Android **converge** hacia ese estado cuando puede
> conectarse. No hay streaming en vivo ni órdenes instantáneas: por eso una
> pantalla apagada no pierde el cambio, y una sin Internet sigue reproduciendo.

## Documentación

| Documento | Contenido |
| --- | --- |
| [docs/guia-proyecto.md](docs/guia-proyecto.md) | Arquitectura, modelo de datos, flujos, contratos de API y roadmap por etapas |
| [docs/division-trabajo.md](docs/division-trabajo.md) | Quién hace qué, convenciones de código y flujo de ramas |

## Stack

| Componente | Tecnología | Estado |
| --- | --- | --- |
| Backend | Spring Boot 4.1.1 + Java 21 + PostgreSQL 16 | En desarrollo |
| Frontend | React + Vite | Pendiente |
| Player | Android / Android TV + Media3 | Pendiente |

## Estructura

```
backend/     API REST (Spring Boot + Maven)
docs/        Guía de proyecto y división de trabajo
docker-compose.yml   PostgreSQL local
```

## Cómo levantarlo

Requisitos: **JDK 21** y **Docker**. Maven no hace falta: el repo trae el wrapper.

**1. Base de datos**

```bash
docker compose up -d
```

Levanta PostgreSQL 16 en `localhost:5432` con base, usuario y contraseña
`signage`. Los datos persisten en un volumen, así que se puede apagar y prender
sin perder nada.

**2. Backend**

```bash
cd backend && ./mvnw spring-boot:run
```

En Windows (PowerShell o CMD):

```bash
cd backend && mvnw.cmd spring-boot:run
```

Queda escuchando en `http://localhost:8080`. Hibernate crea las tablas solo
(`ddl-auto=update`).

### Configuración

Los valores por defecto de `application.properties` ya coinciden con el
`docker-compose.yml`, así que no hay nada que configurar para arrancar. Para
cambiarlos sin tocar el repo, usar variables de entorno:

| Variable | Default |
| --- | --- |
| `SIGNAGE_DB_HOST` | `localhost` |
| `SIGNAGE_DB_PORT` | `5432` |
| `SIGNAGE_DB_NAME` | `signage` |
| `SIGNAGE_DB_USER` | `signage` |
| `SIGNAGE_DB_PASSWORD` | `signage` |

O crear `backend/src/main/resources/application-local.properties`, que está
ignorado por git.

> Las credenciales de este README son solo para desarrollo local. No usar estos
> valores en un servidor accesible desde Internet.
