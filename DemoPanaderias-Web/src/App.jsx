import { useEffect, useState } from 'react';
import { Btn, DialogWindow, OptionPaneProvider } from './ui.jsx';
import { useDb } from './store.js';
import Principal from './Principal.jsx';
import Informes from './Informes.jsx';
import {
  CajaDialog, ComprasDialog, ConfiguracionDialog, MermaDialog, ProductosDialog, ProveedoresDialog, StockDialog,
} from './Dialogs.jsx';

const STAGE_W = 1280;
const STAGE_H = 800;

const DIALOGOS = {
  Caja: { w: 720, h: 520, C: CajaDialog },
  Stock: { w: 760, h: 520, C: StockDialog },
  Proveedores: { w: 720, h: 480, C: ProveedoresDialog },
  Compras: { w: 760, h: 520, C: ComprasDialog },
  Merma: { w: 720, h: 500, C: MermaDialog },
  Productos: { w: 700, h: 500, C: ProductosDialog },
  Informes: { w: 1100, h: 860, C: Informes },
  Configuración: { w: 620, h: 460, C: ConfiguracionDialog },
};

function useStageScale() {
  const calc = () => {
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    return Math.min((vw - 24) / STAGE_W, (vh - 64) / STAGE_H, 1.35);
  };
  const [scale, setScale] = useState(calc);
  useEffect(() => {
    const on = () => setScale(calc());
    window.addEventListener('resize', on);
    return () => window.removeEventListener('resize', on);
  }, []);
  return scale;
}

function Login({ onLogin }) {
  const db = useDb();
  return (
    <div className="login" style={{ backgroundImage: `url(${import.meta.env.BASE_URL}login_bg.jpg)` }}>
      <div className="login-card">
        <div className="login-brand">FEBROS</div>
        <div className="login-brand-sub">SOFTWARE DEVELOPMENT</div>
        <div className="login-product">
          <span style={{ color: 'var(--ink)' }}>Gestión </span>
          <em>panaderías</em>
        </div>
        <div className="login-local">{db.comercio.nombre}</div>
        <Btn kind="primary" onClick={onLogin}>Iniciar sesión</Btn>
      </div>
    </div>
  );
}

export default function App() {
  const scale = useStageScale();
  const [logueado, setLogueado] = useState(false);
  const [dialogo, setDialogo] = useState(null);
  const [portrait, setPortrait] = useState(false);

  useEffect(() => {
    const on = () => setPortrait(window.innerWidth < 760 && window.innerHeight > window.innerWidth);
    on();
    window.addEventListener('resize', on);
    return () => window.removeEventListener('resize', on);
  }, []);

  const d = dialogo ? DIALOGOS[dialogo] : null;

  return (
    <div className="page">
      <div className="demo-badge">
        <b>DEMO</b>
        <span>Gestión panaderías · datos de ejemplo</span>
        <button type="button" onClick={() => window.location.reload()}>Reiniciar</button>
      </div>

      <div className="stage-holder" style={{ width: STAGE_W * scale, height: STAGE_H * scale, marginTop: 36 }}>
        <div className="stage" style={{ transform: `scale(${scale})` }}>
          <OptionPaneProvider>
            <div className="win main">
              <div className="win-body">
                {logueado ? <Principal openDialog={setDialogo} /> : <Login onLogin={() => setLogueado(true)} />}
              </div>
            </div>
            {d && (
              <DialogWindow width={d.w} height={d.h}>
                <d.C onClose={() => setDialogo(null)} />
              </DialogWindow>
            )}
          </OptionPaneProvider>
        </div>
      </div>

      {portrait && <div className="rotate-hint">Girá el teléfono o abrila en una computadora para verla mejor</div>}
    </div>
  );
}
