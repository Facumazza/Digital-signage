import { useEffect, useState } from "react";

/**
 * Muestra el token de una pantalla recién creada o regenerado.
 *
 * Se muestra en un modal que hay que cerrar a propósito, no como un cartel al
 * pie: el backend guarda solo el hash, así que este es el único momento en que
 * el valor existe en claro. Si se pierde, hay que generar otro.
 */
export default function TokenPantalla({
  codigo,
  token,
  onCerrar,
}: {
  codigo: string;
  token: string;
  onCerrar: () => void;
}) {
  const [copiado, setCopiado] = useState(false);

  useEffect(() => {
    function alPresionar(e: KeyboardEvent) {
      if (e.key === "Escape") onCerrar();
    }
    window.addEventListener("keydown", alPresionar);
    return () => window.removeEventListener("keydown", alPresionar);
  }, [onCerrar]);

  async function copiar() {
    try {
      await navigator.clipboard.writeText(token);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2000);
    } catch {
      // Sin HTTPS algunos navegadores bloquean el portapapeles; el token
      // igual está a la vista para copiarlo a mano.
      setCopiado(false);
    }
  }

  return (
    <div className="fondo-modal" onClick={onCerrar}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>Pantalla {codigo}</h2>
        <p className="sutil">
          Cargá estos datos en el dispositivo Android. El token no se vuelve a
          mostrar: si se pierde, hay que generar uno nuevo.
        </p>

        <label>
          Token de acceso
          <input className="mono" readOnly value={token} onFocus={(e) => e.target.select()} />
        </label>

        <div className="fila-form">
          <button onClick={copiar}>{copiado ? "Copiado ✓" : "Copiar token"}</button>
          <button className="secundario" onClick={onCerrar}>
            Listo, lo guardé
          </button>
        </div>
      </div>
    </div>
  );
}
