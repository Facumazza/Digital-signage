import { useEffect, useState } from "react";
import { api, ErrorHttp } from "../api/cliente";
import type { Pantalla, Playlist, Sucursal } from "../api/tipos";

/**
 * Detalle de sucursal: todas las pantallas de un local, su estado y que
 * reproduce cada una.
 *
 * Es la pantalla que la guia marca como la mas util del panel (seccion 11):
 * permite controlar rapido todas las TVs de una sucursal.
 */
export default function Pantallas() {
  const [sucursales, setSucursales] = useState<Sucursal[]>([]);
  const [sucursalId, setSucursalId] = useState<number | null>(null);
  const [pantallas, setPantallas] = useState<Pantalla[]>([]);
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    api
      .get<Sucursal[]>("/api/sucursales")
      .then((datos) => {
        const activas = datos.filter((s) => s.activo);
        setSucursales(activas);
        if (activas.length > 0) setSucursalId(activas[0].id);
      })
      .catch(mostrarError)
      .finally(() => setCargando(false));
  }, []);

  useEffect(() => {
    if (sucursalId === null) return;

    const sucursal = sucursales.find((s) => s.id === sucursalId);

    api
      .get<Pantalla[]>(`/api/pantallas?sucursalId=${sucursalId}`)
      .then(setPantallas)
      .catch(mostrarError);

    // Las playlists son del cliente, no de la sucursal: se piden segun a quien
    // pertenece el local que se esta mirando.
    if (sucursal?.clienteId != null) {
      api
        .get<Playlist[]>(`/api/playlists?clienteId=${sucursal.clienteId}`)
        .then(setPlaylists)
        .catch(mostrarError);
    }
  }, [sucursalId, sucursales]);

  function mostrarError(err: unknown) {
    setError(
      err instanceof ErrorHttp ? err.mensaje : "No se pudo conectar con el servidor",
    );
  }

  /**
   * Aplica una playlist a todas las pantallas del local de una vez.
   *
   * No crea una relación entre sucursal y playlist: escribe la misma playlist
   * en cada pantalla. Por eso después se puede cambiar una sola, y la tabla
   * sigue mostrando qué reproduce cada una.
   */
  async function asignarATodas(playlistId: string) {
    if (sucursalId === null || playlistId === "__") return;
    setError(null);
    try {
      const actualizadas = await api.put<Pantalla[]>(
        `/api/pantallas/sucursal/${sucursalId}/playlist`,
        { playlistId: playlistId === "" ? null : Number(playlistId) },
      );
      const porId = new Map(actualizadas.map((p) => [p.id, p]));
      setPantallas((previas) => previas.map((p) => porId.get(p.id) ?? p));
    } catch (err) {
      mostrarError(err);
    }
  }

  /**
   * Define el estado deseado de la pantalla. No le avisa a la TV: el player lo
   * descubre la proxima vez que consulte su configuracion, por eso funciona
   * aunque este apagada.
   */
  async function asignarPlaylist(pantallaId: number, playlistId: string) {
    setError(null);
    try {
      const actualizada = await api.put<Pantalla>(
        `/api/pantallas/${pantallaId}/playlist`,
        { playlistId: playlistId === "" ? null : Number(playlistId) },
      );
      setPantallas((previas) =>
        previas.map((p) => (p.id === actualizada.id ? actualizada : p)),
      );
    } catch (err) {
      mostrarError(err);
    }
  }

  if (cargando) return <p className="sutil">Cargando…</p>;

  return (
    <>
      <h1>Pantallas</h1>

      {error && <p className="error">{error}</p>}

      {sucursales.length === 0 ? (
        <div className="tarjeta vacio">
          <p>Todavía no hay sucursales cargadas.</p>
          <p className="sutil">
            Una pantalla siempre pertenece a una sucursal, así que hay que crear
            una antes de poder darla de alta.
          </p>
        </div>
      ) : (
        <>
          <label className="selector">
            Sucursal
            <select
              value={sucursalId ?? ""}
              onChange={(e) => setSucursalId(Number(e.target.value))}
            >
              {sucursales.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.nombre}
                  {s.ciudad ? ` — ${s.ciudad}` : ""}
                </option>
              ))}
            </select>
          </label>

          {pantallas.length === 0 ? (
            <div className="tarjeta vacio">
              <p>Esta sucursal no tiene pantallas.</p>
            </div>
          ) : (
            <>
              {pantallas.length > 1 && (
                <div className="tarjeta fila-form">
                  <label className="crecer">
                    Aplicar a las {pantallas.length} pantallas del local
                    <select
                      value="__"
                      onChange={(e) => asignarATodas(e.target.value)}
                    >
                      <option value="__">— elegir playlist —</option>
                      <option value="">— sin contenido —</option>
                      {playlists.map((pl) => (
                        <option key={pl.id} value={pl.id}>
                          {pl.nombre}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>
              )}
            </>
          )}

          {pantallas.length > 0 && (
            <table className="tabla">
              <thead>
                <tr>
                  <th>Estado</th>
                  <th>Pantalla</th>
                  <th>Código</th>
                  <th>Última conexión</th>
                  <th>Reproduciendo</th>
                </tr>
              </thead>
              <tbody>
                {pantallas.map((p) => (
                  <tr key={p.id}>
                    <td>
                      <span className={`estado ${p.estado.toLowerCase()}`}>
                        {p.estado}
                      </span>
                    </td>
                    <td>{p.nombre}</td>
                    <td className="mono">{p.codigo}</td>
                    <td className="sutil">{formatearFecha(p.ultimaConexion)}</td>
                    <td>
                      <select
                        value={p.playlistId ?? ""}
                        onChange={(e) => asignarPlaylist(p.id, e.target.value)}
                      >
                        <option value="">— sin contenido —</option>
                        {playlists.map((pl) => (
                          <option key={pl.id} value={pl.id}>
                            {pl.nombre}
                          </option>
                        ))}
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </>
  );
}

function formatearFecha(fecha: string | null) {
  // null significa que nunca se conecto, que no es lo mismo que hace mucho.
  if (!fecha) return "nunca";
  return new Date(fecha).toLocaleString("es-AR");
}
