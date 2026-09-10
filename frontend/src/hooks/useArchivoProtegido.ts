import { useEffect, useState } from "react";
import { leerToken } from "../api/cliente";

const BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

/**
 * Descarga un archivo que está detrás de autenticación y devuelve una URL
 * usable en <img> o <video>.
 *
 * Ni <img src> ni <video src> pueden llevar el header Authorization: esas
 * peticiones las hace el navegador por su cuenta, sin credenciales, y el
 * backend responde 401. Hay que pedirlo con fetch, que sí acepta headers, y
 * servirlo desde memoria con una URL de blob.
 *
 * La contra es que descarga el archivo entero antes de mostrar nada. Para
 * previsualizar en el panel está bien; para reproducir un video largo sin
 * esperar haría falta que el backend acepte peticiones por rango.
 */
export function useArchivoProtegido(ruta: string | null) {
  const [url, setUrl] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (!ruta) {
      setUrl(null);
      return;
    }

    let cancelado = false;
    let creada: string | null = null;
    setCargando(true);
    setError(false);

    fetch(`${BASE}${ruta}`, {
      headers: { Authorization: `Bearer ${leerToken() ?? ""}` },
    })
      .then((r) => (r.ok ? r.blob() : Promise.reject(r.status)))
      .then((blob) => {
        if (cancelado) return;
        creada = URL.createObjectURL(blob);
        setUrl(creada);
      })
      .catch(() => !cancelado && setError(true))
      .finally(() => !cancelado && setCargando(false));

    // Sin revoke, cada archivo queda en memoria hasta recargar la página.
    return () => {
      cancelado = true;
      if (creada) URL.revokeObjectURL(creada);
    };
  }, [ruta]);

  return { url, cargando, error };
}
