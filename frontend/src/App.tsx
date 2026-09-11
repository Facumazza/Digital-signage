import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { ProveedorSesion, useSesion } from "./auth/SesionContext";
import Layout from "./componentes/Layout";
import Login from "./paginas/Login";
import Pantallas from "./paginas/Pantallas";
import Sucursales from "./paginas/Sucursales";
import Contenidos from "./paginas/Contenidos";
import Playlists from "./paginas/Playlists";
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
            element={
              <Protegida>
                <Layout />
              </Protegida>
            }
          >
            <Route path="/pantallas" element={<Pantallas />} />
            <Route path="/sucursales" element={<Sucursales />} />
            <Route path="/contenidos" element={<Contenidos />} />
            <Route path="/playlists" element={<Playlists />} />
          </Route>

          <Route path="*" element={<Navigate to="/pantallas" replace />} />
        </Routes>
      </BrowserRouter>
    </ProveedorSesion>
  );
}
