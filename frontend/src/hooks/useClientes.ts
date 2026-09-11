import { useEffect, useState } from "react";
import { api } from "../api/cliente";
import type { Sucursal } from "../api/tipos";

export interface ClienteBreve {
  id: number;
  nombre: string;
}

interface ClienteApi {
  id: number;
  nombre: string;
}

/**
 * Clientes que el usuario puede administrar.
 *
 * Se intenta /api/clientes primero, que es la lista real. Ese endpoint exige
 * SUPER_ADMIN y devuelve 403 para un ADMIN_CLIENTE, así que para ese caso se
 * deducen de sus sucursales.
 *
 * La lista real importa: deduciéndola de las sucursales, un cliente que
 * todavía no tiene ninguna no aparecería, y no habría forma de crearle la
 * primera.
 */
export function useClientes() {
  const [clientes, setClientes] = useState<ClienteBreve[]>([]);
  const [clienteId, setClienteId] = useState<number | null>(null);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    let cancelado = false;

    function aplicar(lista: ClienteBreve[]) {
      if (cancelado) return;
      setClientes(lista);
      if (lista.length > 0) setClienteId((previo) => previo ?? lista[0].id);
    }

    api
      .get<ClienteApi[]>("/api/clientes")
      .then((lista) => aplicar(lista.map((c) => ({ id: c.id, nombre: c.nombre }))))
      .catch(() =>
        // 403: no es SUPER_ADMIN. Sus sucursales ya vienen filtradas por el
        // backend, así que alcanzan para saber de qué cliente es.
        api
          .get<Sucursal[]>("/api/sucursales")
          .then((sucursales) => {
            const porId = new Map<number, string>();
            for (const s of sucursales) {
              if (s.clienteId != null && s.clienteNombre) {
                porId.set(s.clienteId, s.clienteNombre);
              }
            }
            aplicar([...porId].map(([id, nombre]) => ({ id, nombre })));
          })
          .catch(() => !cancelado && setClientes([])),
      )
      .finally(() => !cancelado && setCargando(false));

    return () => {
      cancelado = true;
    };
  }, []);

  return { clientes, clienteId, setClienteId, cargando };
}
