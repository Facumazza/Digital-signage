import type { ErrorApi } from "./tipos";

/**
 * Unico punto por donde el panel habla con el backend.
 *
 * Centralizarlo evita repetir en cada pantalla el header del token, el parseo
 * de errores y el manejo del 401.
 */

const BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

const CLAVE_TOKEN = "signage.token";

export function guardarToken(token: string) {
  localStorage.setItem(CLAVE_TOKEN, token);
}

export function leerToken(): string | null {
  return localStorage.getItem(CLAVE_TOKEN);
}

export function borrarToken() {
  localStorage.removeItem(CLAVE_TOKEN);
}

/** Error con el mensaje que mando el backend, no uno generico del navegador. */
export class ErrorHttp extends Error {
  constructor(
    readonly estado: number,
    mensaje: string,
    readonly campos?: Record<string, string>,
  ) {
    super(mensaje);
  }
}

async function pedir<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  const token = leerToken();

  const respuesta = await fetch(`${BASE}${ruta}`, {
    ...opciones,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...opciones.headers,
    },
  });

  // El token expiro o alguien lo borro: la sesion ya no sirve.
  if (respuesta.status === 401 && token) {
    borrarToken();
    window.location.href = "/login";
    throw new ErrorHttp(401, "La sesion expiro");
  }

  if (!respuesta.ok) {
    // Un 500 puede no traer JSON; no queremos que el parseo tape el error real.
    const cuerpo = (await respuesta.json().catch(() => null)) as ErrorApi | null;
    throw new ErrorHttp(
      respuesta.status,
      cuerpo?.mensaje ?? `Error ${respuesta.status}`,
      cuerpo?.campos,
    );
  }

  // 204 No Content no trae cuerpo que parsear.
  if (respuesta.status === 204) {
    return undefined as T;
  }
  return respuesta.json() as Promise<T>;
}

export const api = {
  get: <T>(ruta: string) => pedir<T>(ruta),

  post: <T>(ruta: string, cuerpo?: unknown) =>
    pedir<T>(ruta, { method: "POST", body: JSON.stringify(cuerpo ?? {}) }),

  put: <T>(ruta: string, cuerpo?: unknown) =>
    pedir<T>(ruta, { method: "PUT", body: JSON.stringify(cuerpo ?? {}) }),
};
