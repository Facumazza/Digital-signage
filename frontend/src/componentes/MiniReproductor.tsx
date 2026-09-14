import { useEffect, useState } from "react";
import { api, ErrorHttp } from "../api/cliente";
import type { ItemPlaylist } from "../api/tipos";
import { useArchivoProtegido } from "../hooks/useArchivoProtegido";

interface Props {
  playlistId: number | null;
}

export default function MiniReproductor({ playlistId }: Props) {
  const [items, setItems] = useState<ItemPlaylist[]>([]);
  const [indice, setIndice] = useState(0);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (playlistId === null) {
      setItems([]);
      setIndice(0);
      return;
    }

    let activo = true;

    async function cargar() {
      try {
        const datos = await api.get<ItemPlaylist[]>(
          `/api/playlist-contenidos/playlist/${playlistId}`,
        );

        if (!activo) return;

        const ordenados = [...datos].sort((a, b) => a.orden - b.orden);

        setItems(ordenados);
        setIndice(0);
        setError(false);
      } catch (err) {
        if (!activo) return;

        if (err instanceof ErrorHttp) {
          setError(true);
        } else {
          setError(true);
        }

        setItems([]);
      }
    }

    cargar();

    // Si alguien modifica la playlist, la mini pantalla vuelve a consultar.
    const intervalo = window.setInterval(cargar, 30000);

    return () => {
      activo = false;
      window.clearInterval(intervalo);
    };
  }, [playlistId]);

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

  const item = items[indice];

  return (
    <div className="mini-reproductor">
      <MedioPlaylist
        key={`${item.id}-${item.url}`}
        item={item}
        onTerminar={() => {
          setIndice((actual) => (actual + 1) % items.length);
        }}
      />

      <div className="mini-reproductor-info">
        <span>
          {indice + 1}/{items.length}
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

  useEffect(() => {
    if (!url || item.tipo !== "IMAGEN") return;

    const segundos = item.duracionVisualizacion ?? 10;

    const temporizador = window.setTimeout(() => {
      onTerminar();
    }, segundos * 1000);

    return () => window.clearTimeout(temporizador);
  }, [url, item.tipo, item.duracionVisualizacion, onTerminar]);

  if (cargando) {
    return (
      <div className="mini-medio mini-medio-cargando">
        Cargando…
      </div>
    );
  }

  if (error || !url) {
    return (
      <div className="mini-medio mini-medio-error">
        No se pudo cargar
      </div>
    );
  }

  if (item.tipo === "VIDEO") {
    return (
      <video
        className="mini-medio"
        src={url}
        autoPlay
        muted
        playsInline
        onEnded={onTerminar}
      />
    );
  }

  return (
    <img
      className="mini-medio"
      src={url}
      alt={item.contenidoNombre}
    />
  );
}