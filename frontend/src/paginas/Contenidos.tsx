import { useEffect, useRef, useState, type FormEvent } from "react";
import { api, ErrorHttp } from "../api/cliente";
import type { Contenido } from "../api/tipos";
import { useClientes } from "../hooks/useClientes";
import ImagenProtegida from "../componentes/ImagenProtegida";
import VistaPrevia from "../componentes/VistaPrevia";

/**
 * Tope acordado para los videos. Tiene que coincidir con
 * signage.contenido.duracion-maxima-video del backend, que es quien lo hace
 * cumplir; acá está para avisar antes de subir 300 MB en vano.
 */
const DURACION_MAXIMA_VIDEO = 120;

/** Biblioteca de archivos de un cliente: subir, renombrar y dar de baja. */
export default function Contenidos() {
  const { clientes, clienteId, setClienteId, cargando } = useClientes();

  const [contenidos, setContenidos] = useState<Contenido[]>([]);
  const [nombre, setNombre] = useState("");
  const [archivo, setArchivo] = useState<File | null>(null);
  const [subiendo, setSubiendo] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inputArchivo = useRef<HTMLInputElement>(null);
  const [previsualizando, setPrevisualizando] = useState<Contenido | null>(null);
  const [duracion, setDuracion] = useState<number | null>(null);
  const [midiendo, setMidiendo] = useState(false);

  const esVideo = archivo?.type.startsWith("video/") ?? false;
  const muyLargo = duracion !== null && duracion > DURACION_MAXIMA_VIDEO;

  useEffect(() => {
    if (clienteId === null) return;
    api
      .get<Contenido[]>(`/api/contenidos?clienteId=${clienteId}`)
      .then(setContenidos)
      .catch(mostrarError);
  }, [clienteId]);

  function mostrarError(err: unknown) {
    setError(
      err instanceof ErrorHttp ? err.mensaje : "No se pudo conectar con el servidor",
    );
  }

  /**
   * Mide el video en el navegador, antes de subirlo.
   *
   * Es el único lugar donde se puede: el backend tendría que leer el MP4 con
   * una librería de video, y subir 300 MB para después rechazarlos sería
   * perder varios minutos.
   */
  async function elegirArchivo(nuevo: File | null) {
    setArchivo(nuevo);
    setDuracion(null);
    setError(null);
    if (!nuevo || !nuevo.type.startsWith("video/")) return;

    setMidiendo(true);
    try {
      setDuracion(await medirDuracion(nuevo));
    } catch {
      setError(
        "No se pudo leer la duración del video. Puede estar dañado o en un formato que el navegador no abre.",
      );
    } finally {
      setMidiendo(false);
    }
  }

  async function subir(e: FormEvent) {
    e.preventDefault();
    if (!archivo || clienteId === null || muyLargo) return;

    setError(null);
    setSubiendo(true);
    try {
      const formulario = new FormData();
      formulario.append("archivo", archivo);

      // nombre y clienteId van como query: el endpoint recibe el archivo por
      // multipart y el resto como parametros.
      const parametros = new URLSearchParams({
        nombre: nombre || archivo.name,
        clienteId: String(clienteId),
      });
      if (duracion !== null) parametros.set("duracionSegundos", String(duracion));

      const creado = await api.subir<Contenido>(
        `/api/contenidos?${parametros}`,
        formulario,
      );
      setContenidos((previos) => [...previos, creado]);
      setNombre("");
      setArchivo(null);
      setDuracion(null);
      if (inputArchivo.current) inputArchivo.current.value = "";
    } catch (err) {
      mostrarError(err);
    } finally {
      setSubiendo(false);
    }
  }

  /**
   * Baja logica: el archivo queda en disco. Alguna playlist puede seguir
   * referenciandolo y un player sin conexion puede estar reproduciendolo.
   */
  async function desactivar(id: number) {
    setError(null);
    try {
      await api.del(`/api/contenidos/${id}`);
      setContenidos((previos) => previos.filter((c) => c.id !== id));
    } catch (err) {
      mostrarError(err);
    }
  }

  if (cargando) return <p className="sutil">Cargando…</p>;

  if (clientes.length === 0) {
    return (
      <div className="tarjeta vacio">
        <p>No hay clientes con sucursales cargadas.</p>
        <p className="sutil">
          Los contenidos pertenecen a un cliente, así que hace falta uno antes de
          poder subir archivos.
        </p>
      </div>
    );
  }

  return (
    <>
      <h1>Contenidos</h1>

      <label className="selector">
        Cliente
        <select
          value={clienteId ?? ""}
          onChange={(e) => setClienteId(Number(e.target.value))}
        >
          {clientes.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nombre}
            </option>
          ))}
        </select>
      </label>

      {error && <p className="error">{error}</p>}

      <form className="tarjeta fila-form" onSubmit={subir}>
        <label className="crecer">
          Nombre
          <input
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            placeholder={archivo?.name ?? "Promo septiembre"}
          />
        </label>

        <label className="crecer">
          Archivo
          <input
            ref={inputArchivo}
            type="file"
            accept="video/*,image/*"
            onChange={(e) => elegirArchivo(e.target.files?.[0] ?? null)}
            required
          />
        </label>

        <button type="submit" disabled={subiendo || midiendo || !archivo || muyLargo}>
          {subiendo ? "Subiendo…" : "Subir"}
        </button>
      </form>

      {esVideo && (
        <p className={muyLargo ? "error" : "sutil ayuda"}>
          {midiendo
            ? "Midiendo la duración del video…"
            : muyLargo
              ? `El video dura ${formatearDuracion(duracion!)} y el máximo es ${formatearDuracion(DURACION_MAXIMA_VIDEO)}. Recortalo antes de subirlo: un video largo pesa mucho, tarda en llegar a cada pantalla y ocupa el disco del dispositivo.`
              : duracion !== null
                ? `Duración: ${formatearDuracion(duracion)}. El máximo es ${formatearDuracion(DURACION_MAXIMA_VIDEO)}.`
                : `Los videos pueden durar hasta ${formatearDuracion(DURACION_MAXIMA_VIDEO)}.`}
        </p>
      )}

      {contenidos.length === 0 ? (
        <div className="tarjeta vacio">
          <p>Todavía no hay contenidos para este cliente.</p>
        </div>
      ) : (
        <table className="tabla">
          <thead>
            <tr>
              <th></th>
              <th>Nombre</th>
              <th>Tipo</th>
              <th>Archivo</th>
              <th>Duración</th>
              <th>Tamaño</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {contenidos.map((c) => (
              <tr key={c.id}>
                <td>
                  {c.tipo === "IMAGEN" ? (
                    <ImagenProtegida ruta={c.url} />
                  ) : (
                    <span className="miniatura video">▶</span>
                  )}
                </td>
                <td>
                  <button
                    className="enlace"
                    onClick={() => setPrevisualizando(c)}
                    title="Ver el contenido"
                  >
                    {c.nombre}
                  </button>
                </td>
                <td>
                  <span className="etiqueta">{c.tipo}</span>
                </td>
                <td className="sutil mono">{c.nombreArchivo}</td>
                <td className="sutil">
                  {c.duracionSegundos === null ? "—" : formatearDuracion(c.duracionSegundos)}
                </td>
                <td className="sutil">{formatearTamano(c.tamanoBytes)}</td>
                <td>
                  <button className="secundario" onClick={() => desactivar(c.id)}>
                    Dar de baja
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {previsualizando && (
        <VistaPrevia
          contenido={previsualizando}
          onCerrar={() => setPrevisualizando(null)}
        />
      )}
    </>
  );
}

/**
 * Lee la duración del video sin subirlo: el navegador solo necesita los
 * primeros bytes del archivo para conocer su metadata.
 */
function medirDuracion(archivo: File): Promise<number> {
  return new Promise((resolver, rechazar) => {
    const url = URL.createObjectURL(archivo);
    const video = document.createElement("video");
    video.preload = "metadata";

    const limpiar = () => URL.revokeObjectURL(url);

    video.onloadedmetadata = () => {
      limpiar();
      // Un video de 30,2 segundos se cuenta como 31: recortar para abajo
      // dejaría pasar uno que en realidad supera el máximo.
      if (!Number.isFinite(video.duration)) rechazar(new Error("duración desconocida"));
      else resolver(Math.ceil(video.duration));
    };
    video.onerror = () => {
      limpiar();
      rechazar(new Error("no se pudo leer el video"));
    };

    video.src = url;
  });
}

function formatearDuracion(segundos: number) {
  if (segundos < 60) return `${segundos} s`;
  const minutos = Math.floor(segundos / 60);
  const resto = segundos % 60;
  return resto === 0 ? `${minutos} min` : `${minutos} min ${resto} s`;
}

function formatearTamano(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}
