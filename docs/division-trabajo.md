# División de trabajo y estado del backend

Este documento reemplaza el reparto "uno hace entidades, otro hace services" de la
sección 13 de la [guía](guia-proyecto.md).

**Por qué se cambió:** entidad → DTO → service → controller es una cadena de
dependencias en una sola dirección. Partirla al medio deja a quien hace services
sin poder compilar hasta que el otro suba las entidades. Cortando **vertical**
(cada uno se lleva su dominio completo) los dos avanzan desde el minuto cero y
casi no se pisan archivos.

---

## Estado actual

El backend del panel está completo: las 7 entidades, sus repositories, DTOs,
services y controllers, más el endpoint del player y autenticación.

| Etapa | Estado |
| --- | --- |
| 0 — Base Spring + PostgreSQL | ✅ |
| 1 — Modelo y repositories | ✅ |
| 2 — Contenido + upload y descarga | ✅ |
| 3 — Playlist + versionado | ✅ |
| 4 — API del player (config + heartbeat) | ✅ |
| 5 a 9 — Android, React, prueba remota, robustez | ❌ no arrancados |
| 10 — Seguridad JWT y roles | ✅ (falta cerrar el player) |
| 11 — Producto | ❌ |

**Lo que falta para el MVP no es backend:** son el player Android y el panel
React, que no tienen todavía una sola línea.

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
| `/api/player/**` está abierto | Cualquiera que sepa un código `GRN-XXXX` lee la config de esa pantalla. **Cerrar antes de exponer a Internet** |
| El secreto JWT está en el repo | Es un valor de desarrollo. En producción va por `SIGNAGE_JWT_SECRETO` |
| Sin índices en las columnas FK | PostgreSQL no los crea solo. Se va a notar cuando haya volumen (Etapa 9) |
| `ddl-auto=update` | No borra columnas ni renombra. Producción necesita migraciones versionadas (Etapa 9) |
| Sin tests de services ni controllers | Los 5 que hay cubren solo el mapeo de entidades |

## Checklist antes de pushear

- [ ] `mvnw test` en verde
- [ ] El endpoint probado a mano, caso feliz y al menos un error
- [ ] `git pull` de `develop` hecho y sin conflictos
- [ ] Commit chico y con el porqué, no solo el qué
