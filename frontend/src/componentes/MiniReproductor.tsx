import { useEffect, useRef, useState } from "react";
import { api } from "../api/cliente";
import type { ItemPlaylist } from "../api/tipos";
import { useArchivoProtegido } from "../hooks/useArchivoProtegido";

interface Props {
  playlistId: number | null;
  /** Si la pantalla está apagada, la vista previa muestra negro, como la TV. */
  encendida: boolean;
}

/**
 * Vista previa de lo que muestra una pantalla, dentro de la tabla.
 *
 * Arranca en reposo y reproduce solo al pasar el mouse o hacer clic. Hay una
 * por fila: si todas descargaran y reprodujeran a la vez, una sucursal con
 * diez TVs haría bajar al navegador diez videos completos y correrlos juntos.
 */
export default function MiniReproductor({ playlistId, encendida }: Props) {
  const [items, setItems] = useState<ItemPlaylist[]>([]);
  const [indice, setIndice] = useState(0);
  const [error, setError] = useState(false);
  const [activo, setActivo] = useState(false);
  const firma = useRef("");

  useEffect(() => {
    if (playlistId === null) {
      setItems([]);
      setIndice(0);
      firma.current = "";
      return;
    }

    let vigente = true;

    async function cargar() {
      try {
        const datos = await api.get<ItemPlaylist[]>(
          `/api/playlist-contenidos/playlist/${playlistId}`,
        );
        if (!vigente) return;

        const ordenados = [...datos].sort((a, b) => a.orden - b.orden);

        // Solo se vuelve al principio si la playlist cambió de verdad. Antes
        // cada recarga reiniciaba el índice, y como se recarga cada 30
        // segundos, una playlist más larga nunca pasaba de sus primeros
        // contenidos.
        const nuevaFirma = ordenados
          .map((i) => `${i.id}:${i.orden}:${i.duracionVisualizacion ?? ""}`)
          .join("|");
        if (nuevaFirma !== firma.current) {
          firma.current = nuevaFirma;
          setItems(ordenados);
          setIndice(0);
        }
        setError(false);
      } catch {
        if (!vigente) return;
        setError(true);
        setItems([]);
        firma.current = "";
      }
    }

    cargar();

    // Se refresca solo mientras se está mirando: en reposo alcanza con la
    // carga inicial, y así no hay una consulta cada 30 segundos por fila.
    const intervalo = activo ? window.setInterval(cargar, 30000) : undefined;

    return () => {
      vigente = false;
      if (intervalo) window.clearInterval(intervalo);
    };
  }, [playlistId, activo]);

  const eventos = {
    onMouseEnter: () => setActivo(true),
    onMouseLeave: () => setActivo(false),
    // En pantallas táctiles no hay hover: el clic alterna.
    onClick: () => setActivo((a) => !a),
    title: activo ? "" : "Pasá el mouse para reproducir",
  };

  if (!encendida) {
    return (
      <div className="mini-reproductor mini-reproductor-vacio mini-reproductor-apagada">
        <span>Apagada</span>
      </div>
    );
  }

  if (playlistId === null) {
    return (
      <div className="mini-reproductor mini-reproductor-vacio">
        <span>Sin playlist</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="mini-reproductor mini-reproductor-error">
        <span>No se pudo cargar</span>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="mini-reproductor mini-reproductor-vacio">
        <span>Playlist vacía</span>
      </div>
    );
  }

  const item = items[Math.min(indice, items.length - 1)];

  return (
    <div className="mini-reproductor mini-reproductor-interactivo" {...eventos}>
      {activo ? (
        <MedioPlaylist
          key={`${item.id}-${item.url}`}
          item={item}
          onTerminar={() => setIndice((actual) => (actual + 1) % items.length)}
        />
      ) : (
        // En reposo no se descarga nada: solo se muestra qué hay.
        <div className="mini-medio mini-medio-cargando mini-reposo">▶</div>
      )}

      <div className="mini-reproductor-info">
        <span>
          {Math.min(indice, items.length - 1) + 1}/{items.length}
        </span>
        <span>{item.contenidoNombre}</span>
      </div>
    </div>
  );
}

function MedioPlaylist({
  item,
  onTerminar,
}: {
  item: ItemPlaylist;
  onTerminar: () => void;
}) {
  const { url, cargando, error } = useArchivoProtegido(item.url);

  // La función llega nueva en cada render del padre. Guardarla en una ref
  // evita reiniciar el temporizador de la imagen cada vez que eso pasa.
  const terminar = useRef(onTerminar);
  terminar.current = onTerminar;

  useEffect(() => {
    if (!url || item.tipo !== "IMAGEN") return;

    const segundos = item.duracionVisualizacion ?? 10;
    const temporizador = window.setTimeout(() => terminar.current(), segundos * 1000);
    return () => window.clearTimeout(temporizador);
  }, [url, item.tipo, item.duracionVisualizacion]);

  if (cargando) {
    return <div className="mini-medio mini-medio-cargando">Cargando…</div>;
  }

  if (error || !url) {
    return <div className="mini-medio mini-medio-error">No se pudo cargar</div>;
  }

  if (item.tipo === "VIDEO") {
    return (
      <video
        className="mini-medio"
        src={url}
        autoPlay
        muted
        playsInline
        onEnded={() => terminar.current()}
      />
    );
  }

  return <img className="mini-medio" src={url} alt={item.contenidoNombre} />;
}
