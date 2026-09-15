import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { ProveedorSesion, useSesion } from "./auth/SesionContext";
import Layout from "./componentes/Layout";
import Login from "./paginas/Login";
import Clientes from "./paginas/Clientes";
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

/** Un ADMIN_CLIENTE que entra por URL vuelve a sus pantallas. */
function SoloSuperAdmin({ children }: { children: ReactNode }) {
  const { sesion } = useSesion();
  return sesion?.rol === "SUPER_ADMIN" ? <>{children}</> : <Navigate to="/pantallas" replace />;
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
            <Route
              path="/clientes"
              element={
                <SoloSuperAdmin>
                  <Clientes />
                </SoloSuperAdmin>
              }
            />
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
