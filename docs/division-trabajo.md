# División de trabajo y estado del proyecto

Este documento reemplaza el reparto "uno hace entidades, otro hace services" de la
sección 13 de la [guía](guia-proyecto.md).

**Por qué se cambió:** entidad → DTO → service → controller es una cadena de
dependencias en una sola dirección. Partirla al medio deja a quien hace services
sin poder compilar hasta que el otro suba las entidades. Cortando **vertical**
(cada uno se lleva su dominio completo) los dos avanzan desde el minuto cero y
casi no se pisan archivos.

---

## Estado actual

Las tres partes funcionan de punta a punta **dentro de una misma red**: desde
el panel se cambia la playlist de una pantalla y el celular la descarga y la
reproduce.

| Etapa | Estado |
| --- | --- |
| 0 — Base Spring + PostgreSQL | ✅ |
| 1 — Modelo y repositories | ✅ |
| 2 — Contenido + upload y descarga | ✅ |
| 3 — Playlist + versionado | ✅ |
| 4 — API del player (config + heartbeat) | ✅ |
| 5 — Android mínimo | ✅ |
| 6 — Sincronización: varios contenidos, imágenes, caché y versión | ✅ |
| 7 — Control web | ✅ clientes, usuarios, sucursales, pantallas, contenidos, playlists |
| 8 — Prueba entre dos redes | ❌ el backend solo corre en la LAN |
| 9 — Robustez | ⚠️ el modo offline está programado pero nunca se probó cortando la red |
| 10 — Seguridad: JWT, roles y tokens de dispositivo | ✅ |
| 11 — Producto | ⚠️ autoarranque del player hecho; falta kiosco, despliegue, backups y monitoreo |

**Lo que bloquea el MVP** (sección 15 de la guía) es la Etapa 8: sacar el
backend a Internet, con HTTPS y cambiando la contraseña del admin antes.

### Qué hay en cada parte

- **Backend** (`backend/`): las 7 entidades con su CRUD, aislamiento por
  cliente, bajas lógicas con reactivación, API del player con token por
  dispositivo.
- **Panel** (`frontend/`): React + TypeScript. Clientes y Usuarios los ve solo
  un SUPER_ADMIN. Pantallas permite dar de alta, prender/apagar, asignar
  playlist de a una o a todo el local, y ver una vista previa.
- **Player** (`player-android/`): Kotlin, minSdk 21. Reproduce desde disco,
  guarda la versión recién cuando terminó de bajar todo, se apaga desde el
  panel, se reconfigura manteniendo apretada la pantalla 5 segundos y se abre
  solo al encender el dispositivo.

### Cómo arrancar en una base vacía

1. Levantar el backend: crea solo `admin@grenlus.com` / `admin1234`.
2. Entrar al panel con ese usuario y crear un **cliente** en Clientes.
3. Crearle una **sucursal**, y en Pantallas dar de alta una **pantalla**. El
   panel muestra el token una sola vez.
4. Subir **contenidos**, armar una **playlist** y asignarla a la pantalla.
5. Opcional: en Usuarios, crear un usuario para el cliente.

## Reparto por dominio

| Dominio | Quién | Entidades |
| --- | --- | --- |
| Contenido y reproducción | Facu | `Playlist`, `Contenido`, `Pantalla` |
| Organización y acceso | Lucas | `Sucursal`, `Usuario`, `PlaylistContenido` |
| Base compartida | los dos | `Cliente`, enums, config, seguridad |

`Cliente` no la posee nadie: los dos dominios la referencian. Si hay que
cambiarla, se avisa antes.

**Punto de contacto a coordinar:** `PlaylistContenido` es de Lucas, pero la
regla que incrementa `Playlist.version` al cambiar la composición es lo que
dispara toda la sincronización del player. Cualquier cambio ahí afecta el
dominio de los dos.

## Ramas

```
main                        versiones estables
develop                     integración, siempre compilando
  |-- facu/<tema>
  +-- mila/<tema>
```

Se sale de `develop` y se vuelve a `develop`. Sin pull request: son dos
personas y se hablan todos los días. La contrapartida es que **cada uno es
responsable de no romper `develop`**: compilar y correr `mvnw test` antes de
pushear. Ya pasó tres veces que `develop` quedó sin compilar y bloqueó al otro.

Antes de empezar a trabajar, siempre:

```bash
git checkout develop && git pull
```

## Convenciones

- **Paquetes en minúscula**: `controller`, `service`, `repository`, `entity`, `dtos`, `enums`, `config`, `exception`, `security`.
- **DTOs como `record`**, con sufijo `Dto`, en el paquete `dtos`.
- **Nunca exponer entidades JPA en el controller.** Ni de entrada ni de salida: un `@RequestBody Entidad` deja setear cualquier campo, incluido el `id`, y devolverla arrastra relaciones enteras y datos internos.
- **Entidades con Lombok**: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`.
- **Tablas y columnas en `snake_case`**, campos Java en `camelCase`.
- **El controller no tiene lógica**: valida con `@Valid` y delega.
- **Reglas de negocio en el service**, con `@Transactional` (o `readOnly = true` al leer).
- **Errores tipados**: `RecursoNoEncontradoException` (404) y `ReglaNegocioException` (409). Nunca `RuntimeException`, que termina en 500.
- **Bajas lógicas** (`activo = false`), no `delete`. La excepción es `PlaylistContenido`, que es una relación y sí se borra.

## Deuda pendiente

| Qué | Por qué importa |
| --- | --- |
| El secreto JWT está en el repo | Es un valor de desarrollo. En producción va por `SIGNAGE_JWT_SECRETO` |
| La contraseña del admin es `admin1234` | Está en el repo. **Cambiarla antes de que el backend sea alcanzable desde Internet**: los escaneos automáticos prueban credenciales por defecto en minutos |
| El backend solo corre en la LAN | La Etapa 8 pide probarlo entre dos redes. Se resuelve con un túnel (Cloudflare, ngrok) o desplegando en un servidor |
| El token de pantalla viaja en texto plano | Va en un header, así que **sin HTTPS cualquiera en la red lo lee**. Obligatorio antes de la Etapa 8 |
| Un token de pantalla no expira | Vale hasta que se regenere a mano. Alcanza para el MVP; una rotación automática es de V2 |
| Sin índices en las columnas FK | PostgreSQL no los crea solo. Se va a notar cuando haya volumen (Etapa 9) |
| `ddl-auto=update` | No borra columnas ni renombra. Producción necesita migraciones versionadas (Etapa 9) |
| Sin tests de services ni controllers | Los 5 que hay cubren solo el mapeo de entidades |
| Cambiar una contraseña no cierra las sesiones abiertas | El JWT sigue valiendo hasta que vence (8 h). La baja de un usuario sí corta el acceso en el acto |
| Un usuario no puede cambiarse su propia contraseña | Hoy la cambia un SUPER_ADMIN desde Usuarios |
| El autoarranque en Android 10+ necesita un permiso manual | "Mostrar sobre otras apps" se activa desde la configuración del player, una vez por dispositivo |

## Checklist antes de pushear

- [ ] `mvnw test` en verde
- [ ] Si se tocó el panel: `npm run build` compila (el modo desarrollo no chequea tipos)
- [ ] El endpoint probado a mano, caso feliz y al menos un error
- [ ] `git pull` de `develop` hecho y sin conflictos
- [ ] Commit chico y con el porqué, no solo el qué
