import { useArchivoProtegido } from "../hooks/useArchivoProtegido";

/** Miniatura de un contenido que está detrás de autenticación. */
export default function ImagenProtegida({ ruta, alt = "" }: { ruta: string; alt?: string }) {
  const { url } = useArchivoProtegido(ruta);

  if (!url) return <span className="miniatura" />;
  return <img className="miniatura" src={url} alt={alt} />;
}
