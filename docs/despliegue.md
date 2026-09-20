# Despliegue en Internet — paso a paso

Etapa 8 de la [guía](guia-proyecto.md): sacar el sistema de la red de casa para
que las pantallas funcionen desde cualquier lado.

| Parte | Dónde | Por qué |
| --- | --- | --- |
| Panel React | Vercel | Son archivos estáticos: los sirve con HTTPS y se actualizan solos con cada push |
| Backend + PostgreSQL | Railway | Necesita estar siempre prendido, con base de datos y disco |
| Dominio y DNS | Cloudflare | Apunta cada nombre a donde corresponde |

Los archivos de configuración ya están en el repo: `backend/Dockerfile`,
`backend/railway.json` y `frontend/vercel.json`. Lo que falta es crear las
cuentas y cargar las variables.

**Antes de arrancar, dos cosas que rompen este stack** (están resueltas en los
pasos 3.3 y 5.2, pero conviene saberlas desde ahora):

- **El disco de Railway se borra en cada deploy.** Sin un volumen montado,
  cada versión nueva del backend se lleva puestos los videos e imágenes de
  todos los clientes.
- **Cloudflare no deja subir archivos de más de 100 MB** a través de su proxy.
  Un video de 150 MB fallaría con un error de Cloudflare, no del backend.

---

## 1. Separar el pago del cliente

El problema: en Railway **la facturación es por workspace, no por proyecto**. Si
crean este proyecto dentro del workspace donde está el cliente, el consumo se
cobra a la tarjeta de ese cliente.

La solución es un workspace propio para Grenlus:

1. En railway.com, arriba a la izquierda, abrir el selector de workspace.
2. **Create new workspace** y ponerle `Grenlus`.
3. Entrar a ese workspace nuevo y, en **Settings → Billing**, cargar el medio
   de pago de ustedes. Confirmar que la tarjeta del cliente **no** figura ahí.
4. De ahora en más, **verificar el workspace antes de crear cada cosa**: el
   selector de arriba a la izquierda tiene que decir `Grenlus`.

Alternativa: crear otra cuenta de Railway con otro mail. Queda más separado,
pero son dos cuentas para mantener y hay que invitar a Lucas por separado. Con
el workspace alcanza.

**Sobre los costos.** Railway cobra por uso y el precio cambia seguido, así que
convienen mirarlo en su página antes de cargar la tarjeta. Este proyecto son
tres cosas que consumen: el backend, la base y el disco. El disco es el que más
crece con el tiempo, porque los videos se acumulan. Vercel y Cloudflare, para
lo que necesitamos, tienen plan gratuito; tengan en cuenta que el plan gratis
de Vercel es para uso no comercial, así que cuando le facturen a un cliente hay
que mirar si corresponde pasar al plan pago.

---

## 2. Cloudflare: el dominio

Se hace primero porque la validación del dominio puede demorar, y mientras
tanto se avanza con el resto.

1. Registrar el dominio (por ejemplo `grenlus.com.ar`) o, si ya lo tienen en
   otro lado, agregarlo en Cloudflare y cambiar los nameservers donde lo
   compraron.
2. Los dos nombres que vamos a usar:
   - `panel.grenlus.com.ar` → el panel, en Vercel
   - `api.grenlus.com.ar` → el backend, en Railway
3. Los registros DNS se crean recién en el paso 5, cuando existan las
   direcciones de Vercel y Railway.

Si todavía no tienen dominio, se puede desplegar igual con las direcciones que
dan Vercel y Railway, y agregarlo después sin rehacer nada. Lo único que hay
que tocar al cambiarlas es `VITE_API_URL`, `SIGNAGE_CORS_ORIGENES` y la
dirección del servidor en cada pantalla.

---

## 3. Railway: backend y base de datos

Verificar arriba a la izquierda que el workspace sea `Grenlus`.

### 3.1 Crear el proyecto

1. **New Project → Deploy from GitHub repo → `Digital-signage`**. Si no aparece,
   hay que darle permiso sobre el repo desde la pantalla que ofrece Railway.
2. Va a intentar un primer deploy y **va a fallar**: todavía no sabe que el
   backend está en una subcarpeta. Es esperable.
3. Abrir el servicio → **Settings → Root Directory** → `backend` → Save.

Con eso Railway detecta el `Dockerfile` y el `railway.json` solos: no hay que
elegir lenguaje ni versión de Java.

### 3.2 Agregar PostgreSQL

**New → Database → Add PostgreSQL**, dentro del mismo proyecto.

### 3.3 Agregar el volumen (no saltear)

En el servicio del backend: **New → Volume**, con **Mount path** `/data`.

Sin esto, cada deploy borra todos los archivos subidos. El tamaño se puede
agrandar después, así que arranquen chico.

### 3.4 Cargar las variables

En el servicio del backend, pestaña **Variables**. Las cinco de la base se
escriben como referencias, así siguen funcionando si Railway rota la
contraseña.

| Variable | Valor |
| --- | --- |
| `SIGNAGE_DB_HOST` | `${{Postgres.PGHOST}}` |
| `SIGNAGE_DB_PORT` | `${{Postgres.PGPORT}}` |
| `SIGNAGE_DB_NAME` | `${{Postgres.PGDATABASE}}` |
| `SIGNAGE_DB_USER` | `${{Postgres.PGUSER}}` |
| `SIGNAGE_DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `SIGNAGE_STORAGE_RUTA` | `/data/uploads` |
| `SIGNAGE_JWT_SECRETO` | una cadena larga al azar (abajo el comando) |
| `SIGNAGE_ADMIN_EMAIL` | el mail del primer administrador |
| `SIGNAGE_ADMIN_PASSWORD` | una contraseña propia, **nunca `admin1234`** |
| `SIGNAGE_CORS_ORIGENES` | se completa en el paso 4.3 |

`PORT` la pone Railway sola: no hay que crearla.

**El secreto JWT** firma las sesiones del panel. El que está en el repo es de
desarrollo y cualquiera puede leerlo, así que con ese alguien se fabrica una
sesión de administrador. Generar uno nuevo, en PowerShell:

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

**La contraseña del admin** solo se usa la primera vez que arranca con la base
vacía; después no se vuelve a mirar. Si se la olvidan, se cambia desde la
pantalla Usuarios del panel.

### 3.5 Publicar y probar

1. **Settings → Networking → Generate Domain**. Queda algo como
   `grenlus-backend-production.up.railway.app`.
2. Abrir en el navegador `https://<esa-dirección>/api/salud`. Tiene que
   responder `{"estado":"ok"}`.

Si no responde, el log del deploy dice por qué. Los errores típicos son el Root
Directory sin poner y alguna variable de la base mal escrita.

---

## 4. Vercel: el panel

1. **Add New → Project → importar `Digital-signage`**.
2. **Root Directory**: `frontend`. El `vercel.json` ya indica el framework y
   manda todas las rutas a `index.html`; sin eso, entrar directo a `/pantallas`
   o recargar esa página da 404, porque las rutas las resuelve React en el
   navegador.
3. **Environment Variables**: `VITE_API_URL` con la dirección del backend, sin
   barra final. Al principio la de Railway; después, `https://api.grenlus.com.ar`.
   Se usa **al compilar**: si la cambian, hay que volver a desplegar el panel.
4. Deploy. Va a quedar en una dirección tipo `grenlus.vercel.app`.

### 4.3 Habilitar el panel en el backend

Recién ahora se puede completar `SIGNAGE_CORS_ORIGENES` en Railway, con la
dirección del panel y sin barra final:

```
https://panel.grenlus.com.ar
```

Mientras prueban con las direcciones provisorias, pueden poner las dos
separadas por coma:

```
https://grenlus.vercel.app,https://panel.grenlus.com.ar
```

Sin esto el navegador bloquea las llamadas al backend y el panel se ve pero no
carga nada. Al guardar la variable, Railway reinicia el backend solo.

---

## 5. Conectar el dominio

### 5.1 Decirle a cada servicio cuál es su nombre

- En **Vercel**: Project → Settings → Domains → agregar `panel.grenlus.com.ar`.
- En **Railway**: servicio del backend → Settings → Networking → Custom Domain →
  `api.grenlus.com.ar`.

Cada uno indica qué registro CNAME hay que crear.

### 5.2 Crear los registros en Cloudflare

| Nombre | Tipo | Apunta a | Proxy |
| --- | --- | --- | --- |
| `panel` | CNAME | lo que indique Vercel | Naranja (proxy activado) |
| `api` | CNAME | lo que indique Railway | **Gris (DNS only)** |

**El de `api` va en gris.** Con la nubecita naranja, Cloudflare corta las
subidas de más de 100 MB y no van a poder subir videos grandes desde el panel.
En gris el tráfico va directo a Railway, que igual sirve todo por HTTPS.

En **SSL/TLS → Overview**, dejar el modo en **Full (strict)**.

### 5.3 Cerrar el círculo

1. En Vercel, cambiar `VITE_API_URL` a `https://api.grenlus.com.ar` y volver a
   desplegar.
2. En Railway, dejar `SIGNAGE_CORS_ORIGENES` en `https://panel.grenlus.com.ar`.
3. Entrar al panel, iniciar sesión y verificar que cargue clientes y pantallas.

---

## 6. Las pantallas

En cada dispositivo, mantener apretada la pantalla (o el botón OK del control)
5 segundos y cambiar **Dirección del servidor** por `https://api.grenlus.com.ar`.
El código y el token no cambian, y lo que ya tenía descargado se conserva.

**Antes de comprar TV Box:** los modelos con Android 7.0 o anterior pueden
rechazar los certificados actuales y no conectarse nunca. Si consiguen uno
prestado, conviene probarlo antes de comprar varios.

---

## 7. Después del primer deploy

| Qué | Por qué importa |
| --- | --- |
| **Probar el corte de Internet** | Es el requisito central del producto y todavía no se probó con el backend afuera |
| **Backups de la base** | Sin backup, un error borra clientes y playlists sin vuelta |
| **Backup de los archivos** | El volumen no se respalda solo. Conviene tener los videos originales también fuera del servidor |
| **`ddl-auto=update`** | Hibernate crea y modifica tablas solo. En producción corresponde migraciones versionadas (Flyway o Liquibase) |
| **Sin límite de espacio por cliente** | Nada impide que un cliente llene el disco, y en Railway el disco se paga |
| **Sin monitoreo** | Si el backend se cae, se enteran porque un cliente avisa. `/api/salud` ya sirve para engancharlo a un monitor gratuito |
