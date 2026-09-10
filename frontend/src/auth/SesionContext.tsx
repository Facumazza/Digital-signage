import { createContext, useContext, useState, type ReactNode } from "react";
import { api, borrarToken, guardarToken, leerToken } from "../api/cliente";
import type { LoginResponse, Rol } from "../api/tipos";

/**
 * Sesion del panel.
 *
 * El token se guarda en localStorage para que recargar la pagina no obligue a
 * loguearse de nuevo. Es vulnerable a XSS: cualquier script que se cuele puede
 * leerlo. La alternativa seria una cookie HttpOnly, que el navegador no deja
 * leer por JavaScript, pero eso hay que resolverlo en el backend.
 */

interface Sesion {
  email: string;
  rol: Rol;
}

interface ContextoSesion {
  sesion: Sesion | null;
  autenticado: boolean;
  entrar: (email: string, password: string) => Promise<void>;
  salir: () => void;
}

const Contexto = createContext<ContextoSesion | null>(null);

const CLAVE_SESION = "signage.sesion";

function sesionGuardada(): Sesion | null {
  if (!leerToken()) return null;
  const crudo = localStorage.getItem(CLAVE_SESION);
  return crudo ? (JSON.parse(crudo) as Sesion) : null;
}

export function ProveedorSesion({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(sesionGuardada);

  async function entrar(email: string, password: string) {
    const respuesta = await api.post<LoginResponse>("/api/auth/login", {
      email,
      password,
    });
    guardarToken(respuesta.token);

    const nueva = { email: respuesta.email, rol: respuesta.rol };
    localStorage.setItem(CLAVE_SESION, JSON.stringify(nueva));
    setSesion(nueva);
  }

  function salir() {
    borrarToken();
    localStorage.removeItem(CLAVE_SESION);
    setSesion(null);
  }

  return (
    <Contexto.Provider
      value={{ sesion, autenticado: sesion !== null, entrar, salir }}
    >
      {children}
    </Contexto.Provider>
  );
}

export function useSesion() {
  const contexto = useContext(Contexto);
  if (!contexto) {
    throw new Error("useSesion tiene que usarse dentro de ProveedorSesion");
  }
  return contexto;
}
