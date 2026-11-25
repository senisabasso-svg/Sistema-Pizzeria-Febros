import { useMemo, useRef, useState } from 'react';
import { Btn, Combo, Field, SwingTable, useOptionPane } from './ui.jsx';
import { TIPOS_PRODUCTO, commit, fmtDateTime, fmtTime, formatNum, money, parseNum, useDb, ventasDelDia } from './store.js';

const byName = (a, b) => a.nombre.localeCompare(b.nombre, 'es', { sensitivity: 'base' });
const Lbl = ({ children }) => <span className="lbl">{children}</span>;

/* ---------------- Caja ---------------- */

export function CajaDialog({ onClose }) {
  const db = useDb();
  const op = useOptionPane();
  const [tipo, setTipo] = useState('ENTRADA');
  const [monto, setMonto] = useState('');
  const [concepto, setConcepto] = useState('');
  const [sel, setSel] = useState(-1);
  const caja = db.caja;

  const totales = () => {
    const sum = (t) => caja.movimientos.filter((m) => m.tipo === t).reduce((a, m) => a + m.monto, 0);
    const entradas = sum('ENTRADA');
    const salidas = sum('SALIDA');
    const ventas = ventasDelDia(caja.fechaApertura);
    return { entradas, salidas, ventas, esperado: caja.apertura + ventas + entradas - salidas };
  };

  const abrir = async () => {
    if (caja) {
      op.message('Ya hay una caja abierta. Cerrala antes de abrir otra.');
      return;
    }
    const txt = await op.input('Monto inicial en caja (efectivo al abrir):', '0');
    if (txt == null) return;
    const m = parseNum(txt);
    if (m == null) {
      op.message('Ingresá un monto válido.');
      return;
    }
    db.ultimaCaja += 1;
    db.caja = { id: db.ultimaCaja, apertura: m, fechaApertura: new Date(), movimientos: [] };
    commit();
    op.message(`Caja abierta con $${money(m)}`);
  };

  const cerrar = async () => {
    if (!caja) {
      op.message('No hay caja abierta para cerrar.');
      return;
    }
    const { esperado } = totales();
    const txt = await op.input(`Esperado en caja: $${money(esperado)}\n\n¿Cuánto contaste en efectivo?`, money(esperado));
    if (txt == null) return;
    const contado = parseNum(txt);
    if (contado == null) {
      op.message('Ingresá un monto contado válido.');
      return;
    }
    db.caja = null;
    commit();
    op.message(
      `Caja cerrada.\nEsperado: $${money(esperado)}\nContado: $${money(contado)}\nDiferencia: $${money(contado - esperado)}`,
      'Cierre de caja',
    );
  };

  const registrar = () => {
    if (!caja) {
      op.message('Abrí la caja antes de registrar movimientos.');
      return;
    }
    const m = parseNum(monto);
    if (m == null || m <= 0) {
      op.message('Ingresá un monto mayor a 0.');
      return;
    }
    if (!concepto.trim()) {
      op.message('Ingresá un concepto (ej: pago proveedor, vuelto, gasto).');
      return;
    }
    caja.movimientos.push({ id: caja.movimientos.length + 1, fecha: new Date(), tipo, monto: m, concepto: concepto.trim() });
    commit();
    setMonto('');
    setConcepto('');
  };

  let estado = 'Caja cerrada';
  let color = 'var(--muted)';
  let resumen = 'Abrí la caja para empezar el día y registrar movimientos.';
  if (caja) {
    const t = totales();
    estado = `Caja abierta · #${caja.id}`;
    color = 'var(--success)';
    resumen = `Apertura $${money(caja.apertura)}  ·  Ventas $${money(t.ventas)}  ·  Entradas $${money(t.entradas)}  ·  Salidas $${money(t.salidas)}  ·  Esperado en caja $${money(t.esperado)}`;
  }
  const rows = caja ? [...caja.movimientos].reverse().map((m) => [fmtTime(m.fecha), m.tipo, `$${money(m.monto)}`, m.concepto]) : [];

  return (
    <div className="dlg gap12">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 10 }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4, minWidth: 0 }}>
          <div className="t-heading" style={{ color }}>{estado}</div>
          <div className="t-body" style={{ color: 'var(--muted)', whiteSpace: 'nowrap' }}>{resumen}</div>
        </div>
        <div className="dlg-actions">
          <Btn kind="primary" onClick={abrir}>Abrir caja</Btn>
          <Btn onClick={cerrar}>Cerrar caja</Btn>
        </div>
      </div>
      <SwingTable
        columns={[{ title: 'Hora' }, { title: 'Tipo' }, { title: 'Monto' }, { title: 'Concepto' }]}
        rows={rows}
        selected={sel}
        onSelect={setSel}
      />
      <div className="dlg-form" style={{ gridTemplateColumns: 'auto 120px auto 1fr', justifyContent: 'stretch' }}>
        <Lbl>Tipo</Lbl>
        <Combo value={tipo} options={['ENTRADA', 'SALIDA']} onChange={setTipo} h={32} />
        <Lbl>Monto</Lbl>
        <Field value={monto} onChange={setMonto} h={34} style={{ width: 140 }} onEnter={registrar} />
        <Lbl>Concepto</Lbl>
        <div style={{ gridColumn: 'span 3' }}>
          <Field value={concepto} onChange={setConcepto} h={34} onEnter={registrar} />
        </div>
        <div className="dlg-actions" style={{ gridColumn: 'span 4', justifyContent: 'center' }}>
          <Btn kind="primary" onClick={registrar}>Registrar movimiento</Btn>
          <Btn onClick={onClose}>Cerrar ventana</Btn>
        </div>
      </div>
    </div>
  );
}

/* ---------------- Stock ---------------- */

const CATEGORIAS = ['Ingrediente', 'Producto terminado', 'Packaging', 'Otro'];

export function StockDialog({ onClose }) {
  const db = useDb();
  const op = useOptionPane();
  const [selId, setSelId] = useState(null);
  const [f, setF] = useState({ nombre: '', categoria: 'Ingrediente', cantidad: '', unidad: 'kg', costo: '', minimo: '0' });
  const set = (k) => (v) => setF((s) => ({ ...s, [k]: v }));

  const lista = [...db.stock].sort((a, b) => a.categoria.localeCompare(b.categoria) || byName(a, b));
  const selIndex = lista.findIndex((s) => s.id === selId);

  const seleccionar = (i) => {
    const s = lista[i];
    setSelId(s.id);
    setF({ nombre: s.nombre, categoria: s.categoria, cantidad: formatNum(s.cantidad), unidad: s.unidad, costo: formatNum(s.costo), minimo: formatNum(s.minimo) });
  };

  const leer = () => ({
    nombre: f.nombre.trim(), cantidad: parseNum(f.cantidad), costo: parseNum(f.costo), minimo: parseNum(f.minimo), unidad: f.unidad.trim() || 'u', categoria: f.categoria,
  });

  const guardarNuevo = () => {
    const d = leer();
    if (!d.nombre) return op.message('Ingresá el nombre del ítem.');
    if (d.cantidad == null || d.costo == null || d.minimo == null) return op.message('Cantidad, costo y mínimo deben ser números.');
    db.seq.stock += 1;
    db.stock.push({ id: db.seq.stock, ...d });
    commit();
    setF({ nombre: '', categoria: 'Ingrediente', cantidad: '', unidad: 'kg', costo: '', minimo: '0' });
    return op.message('Ítem de stock guardado.');
  };

  const actualizar = () => {
    const s = db.stock.find((x) => x.id === selId);
    if (!s) return op.message('Seleccioná un ítem de la lista.');
    const d = leer();
    if (!d.nombre || d.cantidad == null || d.costo == null || d.minimo == null) return op.message('Completá nombre, cantidad, costo y mínimo.');
    Object.assign(s, d);
    commit();
    return op.message('Stock actualizado.');
  };

  const ajustar = async (entrada) => {
    const s = db.stock.find((x) => x.id === selId);
    if (!s) return op.message('Seleccioná un ítem.');
    const txt = await op.input(`Cantidad a ${entrada ? 'sumar' : 'restar'}:`, '1');
    if (txt == null) return undefined;
    const delta = parseNum(txt);
    if (delta == null || delta <= 0) return op.message('Ingresá una cantidad válida.');
    s.cantidad += entrada ? delta : -delta;
    commit();
    setF((x) => ({ ...x, cantidad: formatNum(s.cantidad) }));
    return undefined;
  };

  return (
    <div className="dlg">
      <SwingTable
        columns={[{ title: 'Ítem', width: '26%' }, { title: 'Categoría', width: '19%' }, { title: 'Cant.' }, { title: 'Unidad' }, { title: 'Costo u.' }, { title: 'Mínimo' }, { title: 'Alerta' }]}
        rows={lista.map((s) => [s.nombre, s.categoria, formatNum(s.cantidad), s.unidad, `$${formatNum(s.costo)}`, formatNum(s.minimo), s.cantidad <= s.minimo ? 'BAJO' : ''])}
        selected={selIndex}
        onSelect={seleccionar}
      />
      <div className="dlg-form" style={{ gridTemplateColumns: 'auto 190px auto 170px' }}>
        <Lbl>Nombre</Lbl>
        <Field value={f.nombre} onChange={set('nombre')} h={34} />
        <Lbl>Categoría</Lbl>
        <Combo value={f.categoria} options={CATEGORIAS} onChange={set('categoria')} h={34} />
        <Lbl>Cantidad</Lbl>
        <Field value={f.cantidad} onChange={set('cantidad')} h={34} />
        <Lbl>Unidad</Lbl>
        <Field value={f.unidad} onChange={set('unidad')} h={34} />
        <Lbl>Costo unit.</Lbl>
        <Field value={f.costo} onChange={set('costo')} h={34} />
        <Lbl>Mínimo</Lbl>
        <Field value={f.minimo} onChange={set('minimo')} h={34} />
        <div className="dlg-actions" style={{ gridColumn: 'span 4' }}>
          <Btn kind="primary" onClick={guardarNuevo}>Guardar nuevo</Btn>
          <Btn onClick={actualizar}>Actualizar</Btn>
          <Btn onClick={() => ajustar(true)}>+ Entrada</Btn>
          <Btn onClick={() => ajustar(false)}>− Salida</Btn>
          <Btn onClick={onClose}>Cerrar</Btn>
        </div>
      </div>
    </div>
  );
}

/* ---------------- Proveedores ---------------- */

const RUBROS = ['Harinas y granos', 'Lácteos', 'Fiambres', 'Huevos', 'Packaging', 'Bebidas', 'Congelados', 'Limpieza', 'General'];

export function ProveedoresDialog({ onClose }) {
  const db = useDb();
  const op = useOptionPane();
  const vacio = { nombre: '', telefono: '', rubro: 'General', nota: '' };
  const [selId, setSelId] = useState(null);
  const [f, setF] = useState(vacio);
  const set = (k) => (v) => setF((s) => ({ ...s, [k]: v }));
  const lista = [...db.proveedores].sort(byName);

  const guardarNuevo = () => {
    if (!f.nombre.trim()) return op.message('Ingresá el nombre del proveedor.');
    db.seq.proveedor += 1;
    db.proveedores.push({ id: db.seq.proveedor, ...f, nombre: f.nombre.trim() });
    commit();
    setF(vacio);
    return op.message('Proveedor guardado.');
  };
  const actualizar = () => {
    const p = db.proveedores.find((x) => x.id === selId);
    if (!p) return op.message('Seleccioná un proveedor.');
    if (!f.nombre.trim()) return op.message('El nombre es obligatorio.');
    Object.assign(p, f, { nombre: f.nombre.trim() });
    commit();
    return op.message('Proveedor actualizado.');
  };
  const eliminar = async () => {
    if (!db.proveedores.some((x) => x.id === selId)) return op.message('Seleccioná un proveedor.');
    if (!(await op.confirm('¿Eliminar este proveedor?', 'Confirmar'))) return undefined;
    db.proveedores = db.proveedores.filter((x) => x.id !== selId);
    commit();
    setSelId(null);
    setF(vacio);
    return undefined;
  };

  return (
    <div className="dlg">
      <SwingTable
        columns={[{ title: 'Proveedor' }, { title: 'Teléfono' }, { title: 'Rubro' }, { title: 'Nota' }]}
        rows={lista.map((p) => [p.nombre, p.telefono, p.rubro, p.nota])}
        selected={lista.findIndex((p) => p.id === selId)}
        onSelect={(i) => {
          const p = lista[i];
          setSelId(p.id);
          setF({ nombre: p.nombre, telefono: p.telefono, rubro: p.rubro, nota: p.nota });
        }}
      />
      <div className="dlg-form" style={{ gridTemplateColumns: 'auto 200px auto 230px' }}>
        <Lbl>Nombre</Lbl>
        <Field value={f.nombre} onChange={set('nombre')} h={34} />
        <Lbl>Teléfono</Lbl>
        <Field value={f.telefono} onChange={set('telefono')} h={34} />
        <Lbl>Rubro</Lbl>
        <Combo value={f.rubro} options={RUBROS} onChange={set('rubro')} h={34} />
        <Lbl>Nota</Lbl>
        <Field value={f.nota} onChange={set('nota')} h={34} />
        <div className="dlg-actions" style={{ gridColumn: 'span 4' }}>
          <Btn kind="primary" onClick={guardarNuevo}>Guardar nuevo</Btn>
          <Btn onClick={actualizar}>Actualizar</Btn>
          <Btn onClick={eliminar}>Eliminar</Btn>
          <Btn onClick={onClose}>Cerrar</Btn>
        </div>
      </div>
    </div>
  );
}

/* ---------------- Compras ---------------- */

export function ComprasDialog({ onClose }) {
  const db = useDb();
  const op = useOptionPane();
  const proveedores = [...db.proveedores].sort(byName);
  const stock = [...db.stock].sort(byName);
  const [provId, setProvId] = useState(proveedores[0]?.id);
  const [stockId, setStockId] = useState(stock[0]?.id);
  const [cantidad, setCantidad] = useState('');
  const [costo, setCosto] = useState(stock[0] ? money(stock[0].costo) : '');
  const [nota, setNota] = useState('');
  const [sel, setSel] = useState(-1);

  const elegirStock = (id) => {
    setStockId(id);
    const s = db.stock.find((x) => x.id === id);
    if (s) setCosto(money(s.costo));
  };

  const registrar = () => {
    const prov = db.proveedores.find((x) => x.id === provId);
    const s = db.stock.find((x) => x.id === stockId);
    if (!prov) return op.message('Cargá al menos un proveedor antes de comprar.');
    if (!s) return op.message('Cargá ítems en Stock antes de registrar una compra.');
    const cant = parseNum(cantidad);
    const cu = parseNum(costo);
    if (cant == null || cant <= 0) return op.message('Cantidad inválida.');
    if (cu == null || cu < 0) return op.message('Costo unitario inválido.');
    const total = cant * cu;
    db.compras.push({ id: db.compras.length + 1, fecha: new Date(), proveedor: prov.nombre, total, nota: nota.trim() });
    s.cantidad += cant;
    s.costo = cu;
    commit();
    setCantidad('');
    setNota('');
    return op.message(`Compra registrada.\n+${formatNum(cant)} de ${s.nombre}\nTotal $${money(total)}`);
  };

  const hist = [...db.compras].reverse().slice(0, 80);

  return (
    <div className="dlg gap12">
      <div className="dlg-form" style={{ gridTemplateColumns: 'auto 200px auto 200px', gap: '8px 12px' }}>
        <Lbl>Proveedor</Lbl>
        <Combo value={provId} options={proveedores.map((p) => ({ value: p.id, label: p.nombre }))} onChange={setProvId} h={34} />
        <Lbl>Ítem de stock</Lbl>
        <Combo value={stockId} options={stock.map((s) => ({ value: s.id, label: s.nombre }))} onChange={elegirStock} h={34} />
        <Lbl>Cantidad</Lbl>
        <Field value={cantidad} onChange={setCantidad} h={34} style={{ width: 100 }} />
        <Lbl>Costo unit.</Lbl>
        <Field value={costo} onChange={setCosto} h={34} style={{ width: 100 }} />
        <Lbl>Nota</Lbl>
        <div style={{ gridColumn: 'span 3' }}>
          <Field value={nota} onChange={setNota} h={34} />
        </div>
        <div className="dlg-actions" style={{ gridColumn: 'span 4' }}>
          <Btn kind="primary" onClick={registrar}>Registrar compra e ingresar stock</Btn>
          <Btn onClick={() => setSel(-1)}>Actualizar listas</Btn>
        </div>
      </div>
      <SwingTable
        columns={[{ title: 'Fecha' }, { title: 'Proveedor' }, { title: 'Total' }, { title: 'Nota' }]}
        rows={hist.map((c) => [fmtDateTime(c.fecha), c.proveedor, `$${money(c.total)}`, c.nota])}
        selected={sel}
        onSelect={setSel}
      />
      <div className="dlg-footer">
        <span className="t-small">La compra suma cantidad al stock y actualiza el costo unitario del ítem.</span>
        <Btn onClick={onClose}>Cerrar</Btn>
      </div>
    </div>
  );
}

/* ---------------- Merma ---------------- */

const MOTIVOS = ['Sobrante del día', 'Quemado / fallado', 'Vencido', 'Rotura', 'Degustación', 'Otro'];

export function MermaDialog({ onClose }) {
  const db = useDb();
  const op = useOptionPane();
  const stock = [...db.stock].sort(byName);
  const [stockId, setStockId] = useState(stock[0]?.id);
  const [cantidad, setCantidad] = useState('');
  const [motivo, setMotivo] = useState(MOTIVOS[0]);
  const [sel, setSel] = useState(-1);

  const registrar = async () => {
    const s = db.stock.find((x) => x.id === stockId);
    if (!s) return op.message('No hay ítems de stock. Cargá stock primero.');
    const cant = parseNum(cantidad);
    if (cant == null || cant <= 0) return op.message('Ingresá una cantidad válida.');
    if (cant > s.cantidad) {
      const ok = await op.confirm(
        `La merma (${formatNum(cant)}) supera el stock (${formatNum(s.cantidad)}). ¿Registrar igual (stock quedará en 0)?`,
        'Stock insuficiente',
      );
      if (!ok) return undefined;
    }
    const costoEst = cant * s.costo;
    db.mermas.push({ id: db.mermas.length + 1, fecha: new Date(), item: s.nombre, cantidad: cant, unidad: s.unidad, motivo, costo: costoEst });
    s.cantidad = Math.max(0, s.cantidad - cant);
    commit();
    setCantidad('');
    return op.message(`Merma registrada: ${formatNum(cant)} ${s.unidad} de ${s.nombre}\nCosto estimado $${money(costoEst)}`);
  };

  const lista = [...db.mermas].reverse().slice(0, 100);
  const sumaCant = lista.reduce((a, m) => a + m.cantidad, 0);
  const sumaCosto = lista.reduce((a, m) => a + m.costo, 0);

  return (
    <div className="dlg">
      <div className="dlg-form" style={{ gridTemplateColumns: 'auto 220px auto auto', gap: '8px 12px' }}>
        <Lbl>Ítem (stock)</Lbl>
        <Combo value={stockId} options={stock.map((s) => ({ value: s.id, label: `${s.nombre} (${s.unidad})` }))} onChange={setStockId} h={34} />
        <Lbl>Cantidad</Lbl>
        <Field value={cantidad} onChange={setCantidad} h={34} style={{ width: 100 }} onEnter={registrar} />
        <Lbl>Motivo</Lbl>
        <Combo value={motivo} options={MOTIVOS} onChange={setMotivo} h={34} />
        <div className="dlg-actions" style={{ gridColumn: 'span 2' }}>
          <Btn kind="primary" onClick={registrar}>Registrar merma</Btn>
        </div>
      </div>
      <SwingTable
        columns={[{ title: 'Fecha' }, { title: 'Ítem', width: '26%' }, { title: 'Cant.' }, { title: 'Motivo' }, { title: 'Costo est.' }]}
        rows={lista.map((m) => [fmtDateTime(m.fecha), m.item, `${formatNum(m.cantidad)} ${m.unidad}`, m.motivo, `$${money(m.costo)}`])}
        selected={sel}
        onSelect={setSel}
      />
      <div className="dlg-footer">
        <span className="t-heading" style={{ color: 'var(--accent)' }}>
          Merma listada: {formatNum(sumaCant)} u. · Costo est. ${money(sumaCosto)}
        </span>
        <Btn onClick={onClose}>Cerrar</Btn>
      </div>
    </div>
  );
}

/* ---------------- Productos ---------------- */

export function ProductosDialog({ onClose }) {
  const db = useDb();
  const op = useOptionPane();
  const vacio = { nombre: '', tipo: 'Pan', precio: '', costo: '0' };
  const [selId, setSelId] = useState(null);
  const [f, setF] = useState(vacio);
  const set = (k) => (v) => setF((s) => ({ ...s, [k]: v }));
  const lista = useMemo(() => [...db.productos].sort(byName), [db.productos, db.productos.length]);

  const monto = (t) => {
    const l = String(t).replace('$', '').replace(',', '.').trim();
    return /^\d+(\.\d+)?$/.test(l) ? Math.round(Number(l)) : null;
  };
  const precio = monto(f.precio);
  const costo = monto(f.costo);
  let ganTxt = 'Ganancia: —';
  let ganColor = 'var(--success)';
  if (precio != null && costo != null) {
    const g = precio - costo;
    ganTxt = `Ganancia aprox.: $${g}${precio > 0 ? `  (${Math.round((g * 100) / precio)}%)` : ''}`;
    ganColor = g >= 0 ? 'var(--success)' : 'var(--accent)';
  }

  const guardarNuevo = () => {
    if (!f.nombre.trim()) return op.message('Ingresá el nombre del producto.');
    if (precio == null) return op.message('Ingresá un precio de venta válido.');
    db.seq.producto += 1;
    db.productos.push({ id: db.seq.producto, nombre: f.nombre.trim(), tipo: f.tipo, precio, costo: costo ?? 0 });
    commit();
    setF(vacio);
    return op.message('Producto guardado.');
  };
  const actualizar = () => {
    const p = db.productos.find((x) => x.id === selId);
    if (!p) return op.message('Seleccioná un producto de la lista.');
    if (!f.nombre.trim() || precio == null) return op.message('Nombre y precio de venta son obligatorios.');
    Object.assign(p, { nombre: f.nombre.trim(), tipo: f.tipo, precio, costo: costo ?? 0 });
    commit();
    return op.message('Producto actualizado.');
  };

  return (
    <div className="dlg" style={{ padding: 12 }}>
      <SwingTable
        columns={[{ title: 'Producto', width: '34%' }, { title: 'Tipo' }, { title: 'Precio' }, { title: 'Costo' }, { title: 'Ganancia' }]}
        rows={lista.map((p) => [p.nombre, p.tipo, `$${Math.round(p.precio)}`, `$${Math.round(p.costo)}`, `$${Math.round(p.precio) - Math.round(p.costo)}`])}
        selected={lista.findIndex((p) => p.id === selId)}
        onSelect={(i) => {
          const p = lista[i];
          setSelId(p.id);
          setF({ nombre: p.nombre, tipo: p.tipo, precio: String(Math.round(p.precio)), costo: String(Math.round(p.costo)) });
        }}
      />
      <div className="dlg-form" style={{ gridTemplateColumns: 'auto 220px', gap: 8 }}>
        <Lbl>Nombre</Lbl>
        <Field value={f.nombre} onChange={set('nombre')} h={34} />
        <Lbl>Tipo</Lbl>
        <Combo value={f.tipo} options={TIPOS_PRODUCTO} onChange={set('tipo')} h={34} style={{ width: 180 }} />
        <Lbl>Precio venta</Lbl>
        <Field value={f.precio} onChange={set('precio')} h={34} style={{ width: 110 }} />
        <Lbl>Costo producción</Lbl>
        <Field value={f.costo} onChange={set('costo')} h={34} style={{ width: 110 }} />
        <span className="t-bodybold" style={{ gridColumn: 'span 2', color: ganColor }}>{ganTxt}</span>
        <div className="dlg-actions" style={{ gridColumn: 'span 2' }}>
          <Btn kind="primary" onClick={guardarNuevo}>Guardar nuevo</Btn>
          <Btn onClick={actualizar}>Actualizar</Btn>
          <Btn onClick={onClose}>Cerrar</Btn>
        </div>
      </div>
    </div>
  );
}

/* ---------------- Configuración ---------------- */

const IMPRESORAS = ['(Automática)', 'Microsoft Print to PDF', 'POS-80 Térmica', 'EPSON TM-T20II'];

export function ConfiguracionDialog({ onClose }) {
  const db = useDb();
  const op = useOptionPane();
  const c = db.comercio;
  const [nombre, setNombre] = useState(c.nombre);
  const [impresora, setImpresora] = useState(c.impresora);
  const [logo, setLogo] = useState(c.logo ? { url: c.logoUrl || `${import.meta.env.BASE_URL}logo_comercio.png`, name: c.logoName || 'logo_comercio.png' } : null);
  const fileRef = useRef(null);

  const elegir = (e) => {
    const file = e.target.files && e.target.files[0];
    if (file) setLogo({ url: URL.createObjectURL(file), name: file.name });
    e.target.value = '';
  };

  const guardar = async () => {
    if (!nombre.trim()) {
      op.message('Ingresá el nombre del comercio.');
      return;
    }
    Object.assign(c, {
      nombre: nombre.trim(), impresora,
      logo: !!logo, logoUrl: logo ? logo.url : null, logoName: logo ? logo.name : null,
    });
    commit();
    await op.message('Configuración guardada.');
    onClose();
  };

  return (
    <div className="dlg gap12">
      <div className="cfg-scroll">
        <div className="cfg-form">
          <div className="cfg-section">Comercio</div>
          <div className="t-bodybold">Nombre del comercio (aparece en el ticket)</div>
          <Field value={nombre} onChange={setNombre} h={34} />
          <div className="t-bodybold">Logo del comercio</div>
          <div style={{ display: 'flex', gap: 10 }}>
            <div className="cfg-logo">{logo ? <img src={logo.url} alt="" /> : 'Sin logo'}</div>
            <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <div className="dlg-actions">
                <Btn onClick={() => fileRef.current.click()}>Elegir imagen...</Btn>
                <Btn onClick={() => setLogo(null)}>Quitar logo</Btn>
              </div>
              <span className="t-small">{logo ? logo.name : 'Sin logo (se usará el predeterminado si existe)'}</span>
            </div>
            <input ref={fileRef} type="file" accept="image/png,image/jpeg,image/gif" style={{ display: 'none' }} onChange={elegir} />
          </div>
          <div className="t-bodybold">Impresora de tickets</div>
          <Combo value={impresora} options={IMPRESORAS} onChange={setImpresora} />
          <Btn onClick={() => op.message('Lista de impresoras actualizada.')}>Actualizar lista de impresoras</Btn>
        </div>
      </div>
      <div className="dlg-actions" style={{ justifyContent: 'flex-end' }}>
        <Btn onClick={onClose}>Cancelar</Btn>
        <Btn kind="primary" onClick={guardar}>Guardar</Btn>
      </div>
    </div>
  );
}
