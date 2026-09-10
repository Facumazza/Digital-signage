import { useEffect, useRef, useState, type FormEvent } from "react";
import { api, ErrorHttp } from "../api/cliente";
import type { Contenido } from "../api/tipos";
import { useClientes } from "../hooks/useClientes";
import ImagenProtegida from "../componentes/ImagenProtegida";

/** Biblioteca de archivos de un cliente: subir, renombrar y dar de baja. */
export default function Contenidos() {
  const { clientes, clienteId, setClienteId, cargando } = useClientes();

  const [contenidos, setContenidos] = useState<Contenido[]>([]);
  const [nombre, setNombre] = useState("");
  const [archivo, setArchivo] = useState<File | null>(null);
  const [subiendo, setSubiendo] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inputArchivo = useRef<HTMLInputElement>(null);

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

  async function subir(e: FormEvent) {
    e.preventDefault();
    if (!archivo || clienteId === null) return;

    setError(null);
    setSubiendo(true);
    try {
      const formulario = new FormData();
      formulario.append("archivo", archivo);

      // nombre y clienteId van como query: el endpoint recibe el archivo por
      // multipart y el resto como parametros.
      const creado = await api.subir<Contenido>(
        `/api/contenidos?nombre=${encodeURIComponent(nombre || archivo.name)}&clienteId=${clienteId}`,
        formulario,
      );
      setContenidos((previos) => [...previos, creado]);
      setNombre("");
      setArchivo(null);
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
            onChange={(e) => setArchivo(e.target.files?.[0] ?? null)}
            required
          />
        </label>

        <button type="submit" disabled={subiendo || !archivo}>
          {subiendo ? "Subiendo…" : "Subir"}
        </button>
      </form>

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
                <td>{c.nombre}</td>
                <td>
                  <span className="etiqueta">{c.tipo}</span>
                </td>
                <td className="sutil mono">{c.nombreArchivo}</td>
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
    </>
  );
}

function formatearTamano(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}
