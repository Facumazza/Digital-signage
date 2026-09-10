import { useEffect, useState } from "react";
import { api } from "../api/cliente";
import type { Sucursal } from "../api/tipos";

export interface ClienteBreve {
  id: number;
  nombre: string;
}

/**
 * Clientes visibles para el usuario, deducidos de sus sucursales.
 *
 * No se pide /api/clientes a proposito: ese endpoint exige SUPER_ADMIN, y un
 * ADMIN_CLIENTE recibiria 403. Las sucursales las puede listar cualquiera
 * autenticado y ya traen a que cliente pertenecen, asi que alcanzan.
 */
export function useClientes() {
  const [clientes, setClientes] = useState<ClienteBreve[]>([]);
  const [clienteId, setClienteId] = useState<number | null>(null);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    api
      .get<Sucursal[]>("/api/sucursales")
      .then((sucursales) => {
        const porId = new Map<number, string>();
        for (const s of sucursales) {
          if (s.clienteId != null && s.clienteNombre) {
            porId.set(s.clienteId, s.clienteNombre);
          }
        }
        const lista = [...porId].map(([id, nombre]) => ({ id, nombre }));
        setClientes(lista);
        if (lista.length > 0) setClienteId(lista[0].id);
      })
      .catch(() => setClientes([]))
      .finally(() => setCargando(false));
  }, []);

  return { clientes, clienteId, setClienteId, cargando };
}
