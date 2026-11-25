import { useEffect, useMemo, useState } from 'react';
import { Btn, Combo, Field, NumberSpinner, SectionCard, useOptionPane } from './ui.jsx';
import { registrarVenta, useDb } from './store.js';

const NOTAS = ['Sin nota', 'Caliente', 'Para llevar', 'Integral', 'Sin azúcar', 'Sin TACC', 'Encargo'];
const NAV = ['Caja', 'Stock', 'Proveedores', 'Compras', 'Merma', 'Productos', 'Informes', 'Configuración'];

const conNota = (nota) => nota && nota.trim() && nota.toLowerCase() !== 'sin nota';
const itemText = (p) => `${p.cantidad}× ${p.nombre}  $${Math.round(p.precio * p.cantidad)}${conNota(p.nota) ? ` · ${p.nota}` : ''}`;
const ticketLine = (p) => `${p.cantidad} x ${p.nombre}  $${Math.round(p.precio * p.cantidad)}${conNota(p.nota) ? `\n   (${p.nota})` : ''}`;
const center = (t) => (t.length >= 32 ? t : ' '.repeat(Math.floor((32 - t.length) / 2)) + t);
const LINE = '--------------------------------';

export default function Principal({ openDialog }) {
  const db = useDb();
  const op = useOptionPane();
  const productos = useMemo(
    () => [...db.productos].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es', { sensitivity: 'base' })),
    [db.productos.length, db.productos],
  );
  const [productoId, setProductoId] = useState(productos[0]?.id);
  const [nota, setNota] = useState('Sin nota');
  const [cant, setCant] = useState(1);
  const [precio, setPrecio] = useState(String(productos[0]?.precio ?? 0));
  const [direccion, setDireccion] = useState('');
  const [notaCliente, setNotaCliente] = useState('');
  const [items, setItems] = useState([]);
  const [sel, setSel] = useState(-1);

  const producto = db.productos.find((p) => p.id === productoId);
  useEffect(() => {
    if (producto) setPrecio(String(Math.round(producto.precio)));
  }, [productoId, producto?.precio]);

  const total = items.reduce((a, p) => a + p.precio * p.cantidad, 0);

  const agregar = () => {
    if (!producto) {
      op.message('No hay productos cargados. Usá «Productos» para crear el menú.');
      return;
    }
    if (!/^\d+(\.\d+)?$/.test(precio.trim())) {
      op.message('El precio unitario debe ser un número válido.');
      return;
    }
    setItems((l) => [...l, { nombre: producto.nombre, precio: Number(precio), nota, cantidad: cant }]);
    setCant(1);
  };

  const quitar = () => {
    if (sel < 0 || sel >= items.length) {
      op.message('Seleccioná un ítem del pedido.');
      return;
    }
    setItems((l) => l.filter((_, i) => i !== sel));
    setSel(-1);
  };

  const vaciar = async () => {
    if (!items.length) return;
    if (await op.confirm('¿Vaciar el pedido actual?', 'Confirmar')) {
      setItems([]);
      setSel(-1);
    }
  };

  const cobrar = async () => {
    if (!items.length) {
      op.message('Agregá al menos un producto al pedido.');
      return;
    }
    let t = `${LINE}\n${center(db.comercio.nombre)}\n${center('TICKET DE VENTA')}\n${LINE}\n`;
    items.forEach((p) => { t += `${ticketLine(p)}\n`; });
    t += `${LINE}\nITEMS: ${items.length}\nTOTAL: $${Math.round(total)}\n`;
    if (direccion.trim()) t += `DIR: ${direccion.trim()}\n`;
    if (notaCliente.trim()) t += `NOTA: ${notaCliente.trim()}\n`;
    t += `${LINE}\n${center('Gracias · pan fresco del día')}\n`;
    registrarVenta(items);
    await op.textArea(t, 'Vista previa del ticket');
    await op.message('Venta registrada. No hay impresora disponible. Se muestra vista previa.', 'Pedido procesado');
    setItems([]);
    setSel(-1);
    setDireccion('');
    setNotaCliente('');
  };

  const ticketEncargo = async () => {
    if (!items.length && !notaCliente.trim() && !direccion.trim()) {
      op.message('No hay datos para el ticket de repartidor.');
      return;
    }
    if (!direccion.trim() && !(await op.confirm('No hay cliente/dirección. ¿Imprimir igual?', 'Ticket encargo'))) return;
    let detalle = items.map((p) => `${ticketLine(p)}\n`).join('');
    if (notaCliente.trim()) detalle += `NOTA: ${notaCliente.trim()}\n`;
    let t = `${LINE}\n${center(db.comercio.nombre)}\n${center('TICKET ENCARGO')}\n${LINE}\n`;
    t += direccion.trim() ? `DIRECCION:\n${direccion.trim()}\n\n` : 'RETIRO EN LOCAL\n\n';
    if (detalle) t += `${detalle}\n`;
    t += `TOTAL: $${Math.round(total)}\n${LINE}\n`;
    await op.textArea(t, 'Ticket repartidor');
    op.message('No hay impresora disponible. Se muestra vista previa.');
  };

  return (
    <div className="pos">
      <div className="header-bar">
        <div className="pos-brand">
          {db.comercio.logo ? (
            <img className="pos-logo" src={db.comercio.logoUrl || `${import.meta.env.BASE_URL}logo_comercio.png`} alt="" />
          ) : (
            <div className="pos-logo" style={{ color: 'var(--accent)', fontSize: 28, fontWeight: 700, display: 'grid', placeItems: 'center' }}>●</div>
          )}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <div className="t-title">{db.comercio.nombre}</div>
            <div className="t-small">Gestión panaderías · venta · caja · stock</div>
          </div>
        </div>
        <div className="pos-nav">
          {NAV.map((n) => (
            <Btn key={n} kind="nav" onClick={() => openDialog(n)}>{n}</Btn>
          ))}
        </div>
      </div>

      <div className="pos-body">
        <div className="pos-left">
          <SectionCard title="Agregar al pedido">
            <div className="grid-form" style={{ gridTemplateColumns: '58fr 42fr' }}>
              <div className="field-label" style={{ gridColumn: 'span 2' }}>PRODUCTO</div>
              <div style={{ gridColumn: 'span 2' }}>
                <Combo
                  value={productoId}
                  options={productos.map((p) => ({ value: p.id, label: p.nombre }))}
                  onChange={setProductoId}
                />
              </div>
              <div className="field-label">NOTA / DETALLE</div>
              <div className="field-label">CANT.</div>
              <Combo value={nota} options={NOTAS} onChange={setNota} />
              <NumberSpinner value={cant} onChange={setCant} />
              <div className="field-label" style={{ gridColumn: 'span 2' }}>PRECIO UNITARIO</div>
              <div style={{ gridColumn: 'span 2' }}>
                <Field value={precio} onChange={setPrecio} onEnter={agregar} />
              </div>
              <Btn kind="primary" style={{ gridColumn: 'span 2', marginTop: 6 }} onClick={agregar}>+ Sumar al pedido</Btn>
            </div>
          </SectionCard>

          <SectionCard title="Cliente / encargo" bodyStyle={{ justifyContent: 'center' }}>
            <div className="grid-form">
              <div className="field-label">CLIENTE / DIRECCIÓN</div>
              <Field value={direccion} onChange={setDireccion} />
              <div className="field-label">NOTA DEL PEDIDO</div>
              <Field value={notaCliente} onChange={setNotaCliente} />
              <div className="t-small" style={{ marginTop: 4 }}>
                Tip: cobrá en caja, controlá stock y registrá<br />merma del día para no perder margen.
              </div>
            </div>
          </SectionCard>
        </div>

        <SectionCard title="Pedido actual" style={{ flex: 1, minWidth: 0 }} bodyStyle={{ gap: 12 }}>
          <div className="order-list">
            {items.map((p, i) => (
              <div key={i} className={`order-item ${sel === i ? 'sel' : ''}`} onMouseDown={() => setSel(i)}>
                {itemText(p)}
              </div>
            ))}
          </div>
          <div className="total-row">
            <div className="t-smallbold">TOTAL DEL PEDIDO</div>
            <div className="total-value">${Math.round(total)}</div>
          </div>
          <div className="btn-row">
            <Btn onClick={quitar}>Quitar ítem</Btn>
            <Btn onClick={vaciar}>Vaciar</Btn>
            <Btn onClick={ticketEncargo}>Ticket encargo</Btn>
            <Btn kind="primary" onClick={cobrar}>Cobrar e imprimir</Btn>
          </div>
        </SectionCard>
      </div>
    </div>
  );
}
