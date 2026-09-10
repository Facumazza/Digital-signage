/**
 * Espejo en TypeScript de los DTOs del backend.
 *
 * No se generan solos: si alguien renombra un campo en el backend, acá no se
 * entera nadie hasta que algo aparece vacío en pantalla. Cuando el contrato se
 * estabilice conviene generarlos desde OpenAPI.
 */

export type Rol = "SUPER_ADMIN" | "ADMIN_CLIENTE";

export type TipoContenido = "VIDEO" | "IMAGEN";

/** Derivado de ultimaConexion en el backend, no es una columna. */
export type EstadoPantalla = "ONLINE" | "OFFLINE";

export interface LoginResponse {
  token: string;
  email: string;
  rol: Rol;
  expiraEnMs: number;
}

export interface Sucursal {
  id: number;
  nombre: string;
  direccion: string | null;
  ciudad: string | null;
  provincia: string | null;
  activo: boolean;
  clienteId: number | null;
  clienteNombre: string | null;
}

export interface Pantalla {
  id: number;
  codigo: string;
  nombre: string;
  estado: EstadoPantalla;
  ultimaConexion: string | null;
  activo: boolean;
  sucursalId: number;
  sucursalNombre: string;
  playlistId: number | null;
  playlistNombre: string | null;
}

export interface Playlist {
  id: number;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
  fechaCreacion: string;
  version: number;
  clienteId: number;
}

/** Forma unica de los errores de la API. */
export interface ErrorApi {
  momento: string;
  estado: number;
  error: string;
  mensaje: string;
  campos?: Record<string, string>;
}
