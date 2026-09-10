import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { ProveedorSesion, useSesion } from "./auth/SesionContext";
import Login from "./paginas/Login";
import Pantallas from "./paginas/Pantallas";
import type { ReactNode } from "react";

/** Manda al login si no hay sesion. */
function Protegida({ children }: { children: ReactNode }) {
  const { autenticado } = useSesion();
  return autenticado ? <>{children}</> : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <ProveedorSesion>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/pantallas"
            element={
              <Protegida>
                <Pantallas />
              </Protegida>
            }
          />
          <Route path="*" element={<Navigate to="/pantallas" replace />} />
        </Routes>
      </BrowserRouter>
    </ProveedorSesion>
  );
}
