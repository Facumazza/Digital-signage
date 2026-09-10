import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useSesion } from "../auth/SesionContext";
import { ErrorHttp } from "../api/cliente";

export default function Login() {
  const { entrar } = useSesion();
  const navegar = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function manejarEnvio(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setEnviando(true);
    try {
      await entrar(email, password);
      navegar("/pantallas");
    } catch (err) {
      // El backend responde lo mismo si falla el email o la contrasenia, para
      // no revelar que direcciones existen.
      setError(
        err instanceof ErrorHttp
          ? err.mensaje
          : "No se pudo conectar con el servidor",
      );
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="pantalla-centrada">
      <form className="tarjeta login" onSubmit={manejarEnvio}>
        <h1>Grenlus Signage</h1>
        <p className="sutil">Panel de administración</p>

        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoFocus
          />
        </label>

        <label>
          Contraseña
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>

        {error && <p className="error">{error}</p>}

        <button type="submit" disabled={enviando}>
          {enviando ? "Entrando…" : "Entrar"}
        </button>
      </form>
    </div>
  );
}
