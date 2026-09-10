import { useEffect } from "react";
import type { Contenido } from "../api/tipos";
import { useArchivoProtegido } from "../hooks/useArchivoProtegido";

/**
 * Previsualiza un contenido antes de mandarlo a una pantalla.
 *
 * Es lo que evita el circuito de asignar una playlist a una TV y caminar
 * hasta el local para ver si el archivo era el correcto.
 */
export default function VistaPrevia({
  contenido,
  onCerrar,
}: {
  contenido: Contenido;
  onCerrar: () => void;
}) {
  const { url, cargando, error } = useArchivoProtegido(contenido.url);

  // Escape cierra: es lo que espera cualquiera frente a algo que tapa la
  // pantalla.
  useEffect(() => {
    function alPresionar(e: KeyboardEvent) {
      if (e.key === "Escape") onCerrar();
    }
    window.addEventListener("keydown", alPresionar);
    return () => window.removeEventListener("keydown", alPresionar);
  }, [onCerrar]);

  return (
    <div className="fondo-modal" onClick={onCerrar}>
      {/* Un click adentro no debería cerrar. */}
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="barra">
          <div>
            <h2>{contenido.nombre}</h2>
            <p className="sutil">
              {contenido.tipo} · {contenido.nombreArchivo}
            </p>
          </div>
          <button className="secundario" onClick={onCerrar}>
            Cerrar
          </button>
        </div>

        {cargando && <p className="sutil">Descargando…</p>}
        {error && <p className="error">No se pudo cargar el archivo.</p>}

        {url && contenido.tipo === "VIDEO" && (
          // muted no es un capricho: Chrome bloquea el autoplay con sonido, y
          // sin esto el video queda quieto sin decir por que. Los controles
          // permiten subir el volumen.
          <video className="medio" src={url} controls autoPlay muted />
        )}
        {url && contenido.tipo === "IMAGEN" && (
          <img className="medio" src={url} alt={contenido.nombre} />
        )}
      </div>
    </div>
  );
}
