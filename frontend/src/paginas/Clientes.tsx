import { useEffect, useState, type FormEvent } from "react";
import { api, ErrorHttp } from "../api/cliente";
import type { Cliente } from "../api/tipos";

/**
 * Empresas que contratan el servicio.
 *
 * Es el primer paso de todo: sin un cliente no se puede crear una sucursal, y
 * sin sucursal no hay pantallas. Antes de esta pantalla había que crearlos con
 * comandos contra la API.
 *
 * Solo la ve un SUPER_ADMIN: el alta de clientes la hace Grenlus, no el cliente.
 * Muestra también los inactivos, atenuados, para poder reactivarlos.
 */
export default function Clientes() {
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [editando, setEditando] = useState<Cliente | null>(null);
  const [nombre, setNombre] = useState("");
  const [cuit, setCuit] = useState("");
  const [email, setEmail] = useState("");
  const [telefono, setTelefono] = useState("");

  useEffect(() => {
    recargar().finally(() => setCargando(false));
  }, []);

  function recargar() {
    return api
      .get<Cliente[]>("/api/clientes?incluirInactivos=true")
      .then(setClientes)
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
    setCuit("");
    setEmail("");
    setTelefono("");
  }

  function editar(cliente: Cliente) {
    setEditando(cliente);
    setNombre(cliente.nombre);
    setCuit(cliente.cuit ?? "");
    setEmail(cliente.email ?? "");
    setTelefono(cliente.telefono ?? "");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async function guardar(e: FormEvent) {
    e.preventDefault();
    setError(null);

    const cuerpo = {
      nombre,
      cuit: cuit.trim() || null,
      email: email.trim() || null,
      telefono: telefono.trim() || null,
    };

    try {
      if (editando) {
        await api.put<Cliente>(`/api/clientes/${editando.id}`, cuerpo);
      } else {
        await api.post<Cliente>("/api/clientes", cuerpo);
      }
      limpiar();
      await recargar();
    } catch (err) {
      mostrarError(err);
    }
  }

  /**
   * La baja es lógica: sus sucursales, pantallas y contenidos siguen
   * existiendo. Por eso se puede volver atrás.
   */
  async function alternarActivo(cliente: Cliente) {
    if (
      cliente.activo &&
      !window.confirm(
        `¿Dar de baja a ${cliente.nombre}? Deja de aparecer al elegir cliente en el panel. Se puede reactivar.`,
      )
    ) {
      return;
    }
    setError(null);
    try {
      if (cliente.activo) {
        await api.del(`/api/clientes/${cliente.id}`);
      } else {
        await api.post(`/api/clientes/${cliente.id}/reactivar`);
      }
      await recargar();
    } catch (err) {
      mostrarError(err);
    }
  }

  if (cargando) return <p className="sutil">Cargando…</p>;

  return (
    <>
      <h1>Clientes</h1>

      {error && <p className="error">{error}</p>}

      <form className="tarjeta fila-form" onSubmit={guardar}>
        <label className="crecer">
          Nombre
          <input
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            placeholder="Farmacias del Sur"
            maxLength={150}
            required
          />
        </label>
        <label className="crecer">
          CUIT
          <input
            className="mono"
            value={cuit}
            onChange={(e) => setCuit(e.target.value)}
            placeholder="30-12345678-9"
            maxLength={20}
          />
        </label>
        <label className="crecer">
          Email
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="contacto@empresa.com"
            maxLength={150}
          />
        </label>
        <label className="crecer">
          Teléfono
          <input
            value={telefono}
            onChange={(e) => setTelefono(e.target.value)}
            placeholder="11 4444-5555"
            maxLength={50}
          />
        </label>
        <button type="submit">{editando ? "Guardar" : "Crear"}</button>
        {editando && (
          <button type="button" className="secundario" onClick={limpiar}>
            Cancelar
          </button>
        )}
      </form>

      {clientes.length === 0 ? (
        <div className="tarjeta vacio">
          <p>Todavía no hay clientes.</p>
          <p className="sutil">
            Creá el primero arriba. Después se le cargan sucursales, pantallas y
            contenidos.
          </p>
        </div>
      ) : (
        <table className="tabla">
          <thead>
            <tr>
              <th>Estado</th>
              <th>Nombre</th>
              <th>CUIT</th>
              <th>Contacto</th>
              <th>Alta</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {clientes.map((c) => (
              <tr key={c.id} className={c.activo ? "" : "inactiva"}>
                <td>
                  <span className={`estado ${c.activo ? "online" : "offline"}`}>
                    {c.activo ? "ACTIVO" : "INACTIVO"}
                  </span>
                </td>
                <td>{c.nombre}</td>
                <td className="mono sutil">{c.cuit ?? "—"}</td>
                <td className="sutil">
                  {[c.email, c.telefono].filter(Boolean).join(" · ") || "—"}
                </td>
                <td className="sutil">
                  {new Date(c.fechaAlta).toLocaleDateString("es-AR")}
                </td>
                <td className="acciones">
                  <button className="secundario" onClick={() => editar(c)}>
                    Editar
                  </button>
                  <button className="secundario" onClick={() => alternarActivo(c)}>
                    {c.activo ? "Dar de baja" : "Reactivar"}
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
