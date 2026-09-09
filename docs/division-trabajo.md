# División de trabajo — corte vertical por dominio

Este documento reemplaza el reparto "uno hace entidades, otro hace services" de la
sección 13 de la [guía](guia-proyecto.md).

**Por qué se cambió:** entidad → DTO → service → controller es una cadena de
dependencias en una sola dirección. Partirla al medio deja a quien hace services
sin poder compilar hasta que el otro suba las entidades. Cortando **vertical**
(cada uno se lleva su dominio completo) los dos avanzan desde el minuto cero y
casi no se pisan archivos.

---

## Base compartida — ya está en `develop`

Nadie la "posee". Si hay que cambiarla, se avisa antes.

| Archivo | Qué es |
| --- | --- |
| `backend/pom.xml` | Spring Boot 4.1.1, Java 21, Maven |
| `SignageApplication.java` | Arranque |
| `enums/TipoContenido.java`, `enums/Rol.java` | Enums de la sección 4 |
| `entity/Cliente.java` + `ClienteRepository` | Raíz del modelo: los dos dominios la referencian |
| `application.properties` | Conexión a PostgreSQL vía variables de entorno |
| `docker-compose.yml` | PostgreSQL 16 local |

`Cliente` ya está hecha para que ninguno de los dos la duplique, y sirve de
plantilla: así se ven las anotaciones, el `@PrePersist` y el naming de columnas
que vamos a usar en el resto.

---

## Reparto

### Compañero — dominio ORGANIZACIÓN (dónde está cada pantalla)

| Capa | Archivos |
| --- | --- |
| Entidades | `Sucursal`, `Pantalla` |
| Repositories | `SucursalRepository`, `PantallaRepository` |
| DTOs | `SucursalRequest/Response`, `PantallaRequest/Response` |
| Services | `SucursalService`, `PantallaService` |
| Controllers | `/api/sucursales`, `/api/pantallas` |
| Lógica propia | Generación del código `GRN-XXXXXX`, heartbeat, cálculo ONLINE/OFFLINE |

Rama: `feature/sucursal-pantalla`

### Facu — dominio CONTENIDO (qué se reproduce)

| Capa | Archivos |
| --- | --- |
| Entidades | `Contenido`, `Playlist`, `PlaylistContenido` |
| Repositories | `ContenidoRepository`, `PlaylistRepository`, `PlaylistContenidoRepository` |
| DTOs | `ContenidoResponse`, `PlaylistRequest/Response`, `PlaylistItemRequest` |
| Services | `ContenidoService` (upload + storage), `PlaylistService` |
| Controllers | `/api/contenidos`, `/api/playlists` |
| Lógica propia | Guardado de archivos en disco, orden de la playlist, incremento de `version` |

Rama: `feature/contenido-playlist`

### De a dos — puntos de integración

Estos tocan los dos dominios. No los arranque nadie solo.

| Qué | Cuándo | Por qué es conjunto |
| --- | --- | --- |
| Asignar playlist a pantalla | Etapa 7 | Escribe `Pantalla` (él) leyendo `Playlist` (vos) |
| `GET /api/player/{codigo}/config` | Etapa 4 | Lee `Pantalla` + `Playlist` + `Contenido` |
| `Usuario` + seguridad | Etapa 10 | Transversal, va después del circuito completo |

---

## La única dependencia cruzada, y cómo se evita

`Pantalla` tiene que apuntar a `Playlist` (sección 3 de la guía), pero `Playlist`
la construye Facu recién en la Etapa 3. Si el compañero declara el campo ahora,
no compila.

**Acuerdo: `Pantalla` nace sin el campo `playlist`.** Se agrega en la Etapa 3,
cuando `Playlist` ya esté en `develop`. Coincide con el orden del roadmap
—Etapa 1 es Cliente/Sucursal/Pantalla, Etapa 3 es Playlist— así que no se pierde
nada.

```java
// Pantalla.java — Etapa 1: así arranca
@ManyToOne
@JoinColumn(name = "sucursal_id", nullable = false)
private Sucursal sucursal;

// TODO Etapa 3: agregar cuando Playlist exista en develop
// @ManyToOne
// @JoinColumn(name = "playlist_id")
// private Playlist playlist;
```

---

## Convenciones acordadas

Para que el código de los dos se lea igual:

- **Paquetes**: `com.grenlus.signage.<capa>` — `entity`, `repository`, `dto`, `service`, `controller`, `enums`, `config`, `exception`.
- **DTOs como `record`**: son inmutables y no necesitan Lombok. Ej: `public record SucursalResponse(Long id, String nombre, String ciudad) {}`.
- **Entidades con Lombok**: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`.
- **Nunca exponer entidades JPA en el controller.** El controller habla en DTOs; la conversión vive en el service.
- **Tablas y columnas en `snake_case`** (`fecha_alta`, `playlist_id`); los campos Java en `camelCase`.
- **El controller no tiene lógica**: valida con `@Valid` y delega al service.
- **Reglas de negocio siempre en el service**, nunca en el repository ni en la entidad.

## Flujo de git

```
develop                        <- integración, siempre compilando
  |-- feature/sucursal-pantalla    (compañero)
  +-- feature/contenido-playlist   (Facu)
```

- Se sale de `develop` y se vuelve a `develop`. `main` solo recibe versiones estables.
- Commits chicos. Antes de mergear, `git pull origin develop` y resolver conflictos en la propia rama.
- Antes de tocar un archivo de la base compartida: avisar.
- Antes de implementar un endpoint: acordar request y response (regla de la sección 13 de la guía).
