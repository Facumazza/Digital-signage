# Despliegue en Internet

Etapa 8 de la [guía](guia-proyecto.md): sacar el sistema de la red de casa para
que las pantallas funcionen desde cualquier lado.

| Parte | Dónde | Por qué |
| --- | --- | --- |
| Panel React | Vercel | Son archivos estáticos: Vercel los sirve gratis y con HTTPS |
| Backend + PostgreSQL | Railway | Necesita estar siempre prendido, con base de datos y disco |
| Dominio y DNS | Cloudflare | Apunta cada nombre a donde corresponde |

Los archivos de configuración ya están en el repo: `backend/Dockerfile`,
`backend/railway.json` y `frontend/vercel.json`. Lo que falta es crear las
cuentas y cargar las variables.

---

## 1. Antes de empezar: dos cosas que rompen este stack

**El disco de Railway se borra en cada deploy.** Los videos e imágenes se
guardan en el disco del servidor. Si no se monta un **volumen**, cada vez que
suban una versión nueva del backend desaparecen todos los contenidos de todos
los clientes, y las pantallas que todavía no los descargaron quedan sin nada.
Está resuelto en el paso 2.4.

**Cloudflare no deja subir archivos de más de 100 MB.** Es un límite del plan
gratuito, y aplica a todo lo que pase por su proxy (la nubecita naranja). Un
video de 150 MB subido desde el panel fallaría con un error de Cloudflare, no
del backend. Está resuelto en el paso 4.2.

---

## 2. Railway: backend y base de datos

1. **Crear el proyecto** desde el repo de GitHub, eligiendo `Digital-signage`.
2. **Configurar el servicio del backend:** en Settings, poner **Root Directory**
   en `backend`. Railway detecta el `Dockerfile` y el `railway.json` solos, así
   que no hay que elegir lenguaje ni versión de Java.
3. **Agregar PostgreSQL:** botón *New* → *Database* → *PostgreSQL*, dentro del
   mismo proyecto.
4. **Agregar el volumen para los archivos:** en el servicio del backend,
   *New Volume*, con **Mount path** `/data`. Sin esto se pierden los contenidos
   en cada deploy.
5. **Cargar las variables** del servicio del backend (paso 3).
6. **Generar el dominio:** Settings → Networking → *Generate Domain*. Queda algo
   como `grenlus-backend-production.up.railway.app`. Sirve para probar antes de
   tener el dominio propio.

Con eso, `https://<dominio>/api/salud` tiene que responder `{"estado":"ok"}`.

---

## 3. Variables del backend en Railway

Las cuatro de la base se escriben con referencias a la base del mismo proyecto,
así siguen funcionando si Railway rota la contraseña.

| Variable | Valor |
| --- | --- |
| `SIGNAGE_DB_HOST` | `${{Postgres.PGHOST}}` |
| `SIGNAGE_DB_PORT` | `${{Postgres.PGPORT}}` |
| `SIGNAGE_DB_NAME` | `${{Postgres.PGDATABASE}}` |
| `SIGNAGE_DB_USER` | `${{Postgres.PGUSER}}` |
| `SIGNAGE_DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `SIGNAGE_STORAGE_RUTA` | `/data/uploads` |
| `SIGNAGE_JWT_SECRETO` | una cadena larga al azar, ver abajo |
| `SIGNAGE_CORS_ORIGENES` | la dirección del panel, sin barra final |
| `SIGNAGE_ADMIN_EMAIL` | el mail del primer administrador |
| `SIGNAGE_ADMIN_PASSWORD` | una contraseña propia, **nunca `admin1234`** |

`PORT` la pone Railway sola: no hay que crearla.

**El secreto JWT** firma las sesiones del panel. El del repo es de desarrollo y
está a la vista de cualquiera, así que en producción va uno nuevo. Para
generarlo, en PowerShell:

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

**La contraseña del admin** solo se usa la primera vez que arranca con la base
vacía; después no se vuelve a mirar. Si la base ya tiene usuarios y quieren
cambiarla, se hace desde la pantalla Usuarios del panel.

---

## 4. Vercel y Cloudflare

### 4.1 Panel en Vercel

1. Importar el repo y poner **Root Directory** en `frontend`. El `vercel.json`
   ya indica el framework y manda todas las rutas a `index.html`: sin eso,
   entrar directo a `/pantallas` o recargar esa página da 404, porque las rutas
   las resuelve React en el navegador.
2. Agregar la variable **`VITE_API_URL`** con la dirección del backend, por
   ejemplo `https://api.grenlus.com.ar`, sin barra final.
3. Esa variable se usa **al compilar**, no al ejecutar: si la cambian después,
   hay que volver a desplegar el panel para que tome el valor nuevo.

### 4.2 DNS en Cloudflare

| Nombre | Tipo | Apunta a | Proxy |
| --- | --- | --- | --- |
| `panel` | CNAME | el dominio que da Vercel | Naranja (proxy activado) |
| `api` | CNAME | el dominio que da Railway | **Gris (DNS only)** |

**El proxy de `api` tiene que quedar en gris.** Con la nubecita naranja,
Cloudflare corta las subidas de más de 100 MB y el panel no puede subir videos
grandes. En gris, el tráfico va directo a Railway, que igual sirve el sitio por
HTTPS.

Después hay que agregar cada nombre como dominio propio en Vercel y en Railway,
para que emitan el certificado.

---

## 5. Las pantallas

En cada dispositivo, mantener apretada la pantalla (o el botón OK) 5 segundos y
cambiar **Dirección del servidor** por `https://api.grenlus.com.ar`. El código y
el token no cambian, y lo que ya tenía descargado se conserva.

**Antes de comprar TV Box:** los modelos con Android 7.0 o anterior pueden
rechazar los certificados que usan Vercel y Railway, y no conectarse nunca. Si
consiguen uno prestado, conviene probarlo antes de comprar varios.

---

## 6. Lo que queda pendiente después de desplegar

| Qué | Por qué importa |
| --- | --- |
| **Backups de la base** | Railway hace backups en los planes pagos. Sin backup, un error borra clientes y playlists sin vuelta |
| **Backup de los archivos** | El volumen no se respalda solo. Los videos originales conviene tenerlos también fuera del servidor |
| **`ddl-auto=update`** | Hibernate crea y modifica tablas solo. En producción corresponde migraciones versionadas (Flyway o Liquibase) |
| **Sin límite de espacio por cliente** | Nada impide que un cliente llene el disco del servidor |
| **Sin monitoreo** | Si el backend se cae, se enteran porque un cliente avisa. `/api/salud` ya sirve para engancharlo a un monitor gratuito |
