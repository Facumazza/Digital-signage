import { useEffect, useState, type FormEvent } from "react";
import { api, ErrorHttp } from "../api/cliente";
import type { Sucursal } from "../api/tipos";
import { useClientes } from "../hooks/useClientes";

/**
 * Locales de un cliente.
 *
 * Muestra también las inactivas, atenuadas. Esconderlas dejaría sin forma de
 * reabrir un local cerrado: la baja es lógica justamente para que se pueda
 * volver.
 */
export default function Sucursales() {
  const { clientes, clienteId, setClienteId, cargando } = useClientes();

  const [sucursales, setSucursales] = useState<Sucursal[]>([]);
  const [editando, setEditando] = useState<Sucursal | null>(null);
  const [nombre, setNombre] = useState("");
  const [ciudad, setCiudad] = useState("");
  const [direccion, setDireccion] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    recargar();
  }, [clienteId]);

  function recargar() {
    api
      .get<Sucursal[]>("/api/sucursales")
      .then((todas) =>
        setSucursales(
          clienteId === null ? todas : todas.filter((s) => s.clienteId === clienteId),
        ),
      )
      .catch(mostrarError);
  }

  function mostrarError(err: unknown) {
    setError(
      err instanceof ErrorHttp ? err.mensaje : "No se pudo conectar con el servidor",
    );
  }

  function limpiar() {
    setEditando(null);
    setNombre("");
    setCiudad("");
    setDireccion("");
  }

  function editar(sucursal: Sucursal) {
    setEditando(sucursal);
    setNombre(sucursal.nombre);
    setCiudad(sucursal.ciudad ?? "");
    setDireccion(sucursal.direccion ?? "");
  }

  async function guardar(e: FormEvent) {
    e.preventDefault();
    if (clienteId === null) return;
    setError(null);

    const cuerpo = { nombre, ciudad: ciudad || null, direccion: direccion || null };

    try {
      if (editando) {
        await api.put<Sucursal>(`/api/sucursales/${editando.id}`, cuerpo);
      } else {
        await api.post<Sucursal>(`/api/sucursales/cliente/${clienteId}`, cuerpo);
      }
      limpiar();
      recargar();
    } catch (err) {
      mostrarError(err);
    }
  }

  /**
   * Una sucursal inactiva no borra nada: sus pantallas siguen existiendo y sus
   * datos también. Por eso se puede volver atrás.
   */
  async function alternarActiva(sucursal: Sucursal) {
    setError(null);
    try {
      if (sucursal.activo) {
        await api.del(`/api/sucursales/${sucursal.id}`);
      } else {
        await api.post(`/api/sucursales/${sucursal.id}/reactivar`);
      }
      recargar();
    } catch (err) {
      mostrarError(err);
    }
  }

  if (cargando) return <p className="sutil">Cargando…</p>;

  if (clientes.length === 0) {
    return (
      <div className="tarjeta vacio">
        <p>No hay clientes cargados.</p>
        <p className="sutil">Una sucursal siempre pertenece a un cliente.</p>
      </div>
    );
  }

  return (
    <>
      <h1>Sucursales</h1>

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

      <form className="tarjeta fila-form" onSubmit={guardar}>
        <label className="crecer">
          Nombre
          <input
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            placeholder="Sucursal Centro"
            required
          />
        </label>
        <label className="crecer">
          Ciudad
          <input
            value={ciudad}
            onChange={(e) => setCiudad(e.target.value)}
            placeholder="CABA"
          />
        </label>
        <label className="crecer">
          Dirección
          <input
            value={direccion}
            onChange={(e) => setDireccion(e.target.value)}
            placeholder="Av. Corrientes 1234"
          />
        </label>
        <button type="submit">{editando ? "Guardar" : "Crear"}</button>
        {editando && (
          <button type="button" className="secundario" onClick={limpiar}>
            Cancelar
          </button>
        )}
      </form>

      {sucursales.length === 0 ? (
        <div className="tarjeta vacio">
          <p>Este cliente todavía no tiene sucursales.</p>
        </div>
      ) : (
        <table className="tabla">
          <thead>
            <tr>
              <th>Estado</th>
              <th>Nombre</th>
              <th>Ciudad</th>
              <th>Dirección</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {sucursales.map((s) => (
              <tr key={s.id} className={s.activo ? "" : "inactiva"}>
                <td>
                  <span className={`estado ${s.activo ? "online" : "offline"}`}>
                    {s.activo ? "ACTIVA" : "INACTIVA"}
                  </span>
                </td>
                <td>{s.nombre}</td>
                <td className="sutil">{s.ciudad ?? "—"}</td>
                <td className="sutil">{s.direccion ?? "—"}</td>
                <td className="acciones">
                  <button className="secundario" onClick={() => editar(s)}>
                    Editar
                  </button>
                  <button className="secundario" onClick={() => alternarActiva(s)}>
                    {s.activo ? "Dar de baja" : "Reactivar"}
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
