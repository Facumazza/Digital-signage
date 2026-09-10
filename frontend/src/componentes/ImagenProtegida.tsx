import { useEffect, useState } from "react";
import { leerToken } from "../api/cliente";

const BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

/**
 * Muestra una imagen que está detrás de autenticación.
 *
 * Un <img src="..."> no puede llevar el header Authorization: el navegador
 * hace esa petición por su cuenta y sin credenciales, así que el backend
 * responde 401 y Chrome la descarta con ERR_BLOCKED_BY_ORB.
 *
 * La solución es pedir el archivo con fetch, que sí acepta headers, y mostrar
 * el resultado desde memoria con una URL de blob.
 */
export default function ImagenProtegida({
  ruta,
  alt = "",
}: {
  ruta: string;
  alt?: string;
}) {
  const [src, setSrc] = useState<string | null>(null);

  useEffect(() => {
    let cancelado = false;
    let urlCreada: string | null = null;

    fetch(`${BASE}${ruta}`, {
      headers: { Authorization: `Bearer ${leerToken() ?? ""}` },
    })
      .then((r) => (r.ok ? r.blob() : Promise.reject(r.status)))
      .then((blob) => {
        if (cancelado) return;
        urlCreada = URL.createObjectURL(blob);
        setSrc(urlCreada);
      })
      .catch(() => setSrc(null));

    // Sin el revoke, cada blob queda en memoria hasta que se recargue la
    // pagina. Con una lista larga de contenidos eso se acumula rapido.
    return () => {
      cancelado = true;
      if (urlCreada) URL.revokeObjectURL(urlCreada);
    };
  }, [ruta]);

  if (!src) return <span className="miniatura" />;
  return <img className="miniatura" src={src} alt={alt} />;
}
