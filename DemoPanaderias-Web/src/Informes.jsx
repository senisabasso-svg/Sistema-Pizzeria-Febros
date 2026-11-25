import { useEffect, useState } from 'react';
import { Btn, Combo, DateSpinner, DonutChart, Field, PALETTE, fmtDia, useOptionPane } from './ui.jsx';
import { TIPOS_PRODUCTO, dayKey, money, parseNum, useDb } from './store.js';

const TIPOS = ['Todos', ...TIPOS_PRODUCTO];

function hoy() {
  const d = new Date();
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}
function haceDias(n) {
  const d = hoy();
  d.setDate(d.getDate() - n);
  return d;
}

function calcular(db, desde, hasta, tipo, ganMin) {
  const filtrarTipo = tipo !== 'Todos';
  const porTipo = new Map();
  const filas = [];
  let sumVentas = 0;
  let sumCosto = 0;
  let hayDetalle = false;

  for (let d = new Date(hasta); d >= desde; d.setDate(d.getDate() - 1)) {
    const dia = db.ventas[dayKey(d)];
    if (!dia) continue;
    let v = 0;
    let c = 0;
    let tiene = false;
    for (const t of Object.keys(dia).sort()) {
      if (filtrarTipo && t !== tipo) continue;
      tiene = true;
      v += dia[t].ventas;
      c += dia[t].costo;
      porTipo.set(t, (porTipo.get(t) || 0) + dia[t].ventas);
    }
    if (!tiene) continue;
    hayDetalle = true;
    const g = v - c;
    if (ganMin != null && g < ganMin) continue;
    filas.push([fmtDia(d), `$${money(v)}`, `$${money(c)}`, `$${money(g)}`, `${Math.round(v > 0 ? (g * 100) / v : 0)}%`]);
    sumVentas += v;
    sumCosto += c;
  }

  let hint = 'Detalle por producto · filtro tipo/ganancia aplicados al período.';
  if (!hayDetalle && filtrarTipo) hint = `No hay ventas del tipo «${tipo}» en ese rango.`;
  if (!filas.length) {
    hint = ganMin != null
      ? `Ningún día supera la ganancia mínima de $${money(ganMin)}.`
      : 'Sin movimientos en ese rango con los filtros actuales.';
  }

  const ganancia = sumVentas - sumCosto;
  const margen = sumVentas > 0 ? (ganancia * 100) / sumVentas : 0;
  const slicesTipo = [...porTipo.entries()].map(([label, value], i) => ({ label, value, color: PALETTE[i % PALETTE.length] }));
  const slicesRes = [];
  if (sumCosto > 0) slicesRes.push({ label: 'Costo', value: sumCosto, color: '#8B5E3C' });
  if (ganancia > 0) slicesRes.push({ label: 'Ganancia', value: ganancia, color: '#3DDC97' });
  else if (ganancia < 0) slicesRes.push({ label: 'Pérdida', value: -ganancia, color: '#FF8A1F' });

  return { filas, hint, sumVentas, sumCosto, ganancia, margen, slicesTipo, slicesRes };
}

const xml = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

function spreadsheetXml(rows) {
  let out = '<?xml version="1.0" encoding="UTF-8"?>\r\n<?mso-application progid="Excel.Sheet"?>\r\n'
    + '<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"\r\n xmlns:o="urn:schemas-microsoft-com:office:office"\r\n'
    + ' xmlns:x="urn:schemas-microsoft-com:office:excel"\r\n xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">\r\n'
    + '<Styles>\r\n<Style ss:ID="Default" ss:Name="Normal"><Font ss:FontName="Calibri" ss:Size="11"/></Style>\r\n'
    + '<Style ss:ID="Title"><Font ss:FontName="Calibri" ss:Size="14" ss:Bold="1"/></Style>\r\n'
    + '<Style ss:ID="Header"><Font ss:Bold="1"/><Interior ss:Color="#FFE6CC" ss:Pattern="Solid"/></Style>\r\n'
    + '</Styles>\r\n<Worksheet ss:Name="Informe">\r\n<Table>\r\n';
  for (const row of rows) {
    const style = row[0]?.startsWith('__STYLE__') ? row[0].slice(9) : null;
    out += '<Row>\r\n';
    for (const value of style ? row.slice(1) : row) {
      const num = /^-?\d+(\.\d+)?$/.test(value);
      out += `<Cell${style ? ` ss:StyleID="${style}"` : ''}><Data ss:Type="${num ? 'Number' : 'String'}">${xml(value)}</Data></Cell>\r\n`;
    }
    out += '</Row>\r\n';
  }
  return `${out}</Table>\r\n</Worksheet>\r\n</Workbook>\r\n`;
}

export default function Informes({ onClose }) {
  const db = useDb();
  const op = useOptionPane();
  const [desde, setDesde] = useState(haceDias(29));
  const [hasta, setHasta] = useState(hoy());
  const [tipo, setTipo] = useState('Todos');
  const [ganMin, setGanMin] = useState('');
  const [res, setRes] = useState(null);
  const [aplicado, setAplicado] = useState(null);
  const [sel, setSel] = useState(-1);

  const buscar = (d = desde, h = hasta) => {
    if (d > h) {
      op.message('La fecha «Desde» no puede ser posterior a «Hasta».');
      return;
    }
    setRes(calcular(db, d, h, tipo, parseNum(ganMin)));
    setAplicado({ desde: d, hasta: h, tipo });
    setSel(-1);
  };

  useEffect(() => { buscar(); }, []);

  const rango = (d, h) => {
    setDesde(d);
    setHasta(h);
    buscar(d, h);
  };
  const inicioMes = () => {
    const t = hoy();
    return new Date(t.getFullYear(), t.getMonth(), 1);
  };

  const exportar = async () => {
    if (!res || !res.filas.length) {
      op.message('No hay datos para exportar. Aplicá un rango con ventas.');
      return;
    }
    const f = (d) => fmtDia(d).replace(/\//g, '-');
    const nombre = `informe_${f(aplicado.desde)}_${f(aplicado.hasta)}.xls`;
    const rows = [
      ['__STYLE__Title', db.comercio.nombre],
      ['Informe de ventas y ganancia'],
      ['Desde', fmtDia(aplicado.desde)],
      ['Hasta', fmtDia(aplicado.hasta)],
      ['Tipo', aplicado.tipo],
      [''],
      ['__STYLE__Header', 'Resumen', 'Monto'],
      ['Ventas', money(res.sumVentas)],
      ['Costo', money(res.sumCosto)],
      ['Ganancia', money(res.ganancia)],
      ['Margen %', String(Math.round(res.margen))],
      [''],
      ['__STYLE__Header', 'Fecha', 'Ventas', 'Costo', 'Ganancia', 'Margen'],
      ...res.filas.map((r) => [r[0], r[1].replace('$', ''), r[2].replace('$', ''), r[3].replace('$', ''), r[4].replace('%', '')]),
    ];
    const blob = new Blob([spreadsheetXml(rows)], { type: 'application/vnd.ms-excel' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = nombre;
    a.click();
    setTimeout(() => URL.revokeObjectURL(a.href), 2000);
    op.message(`Informe exportado:\n${nombre}\n\nLo encontrás en tu carpeta de Descargas.`, 'Excel');
  };

  const vista = async () => {
    if (!res || !res.filas.length) {
      op.message('No hay datos para mostrar.');
      return;
    }
    let t = `${db.comercio.nombre}\nINFORME DE VENTAS Y GANANCIA\nDesde: ${fmtDia(aplicado.desde)}\nHasta: ${fmtDia(aplicado.hasta)}\nTipo: ${aplicado.tipo}\n`;
    t += `--------------------------------\nVentas:    $${money(res.sumVentas)}\nCosto:     $${money(res.sumCosto)}\nGanancia:  $${money(res.ganancia)}\n--------------------------------\n`;
    res.filas.forEach((r) => { t += `${r[0]}  ${r[1]}  gan ${r[3]}  ${r[4]}\n`; });
    const i = await op.textArea(t, 'Vista del informe', ['Imprimir', 'Cerrar'], 1);
    if (i === 0) op.message('No hay impresora disponible. Se muestra vista previa.');
  };

  const r = res || { filas: [], hint: ' ', sumVentas: 0, sumCosto: 0, ganancia: 0, margen: 0, slicesTipo: [], slicesRes: [] };

  return (
    <div className="dlg" style={{ padding: '12px 14px' }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div className="inf-top">
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 10 }}>
            <span className="t-heading">Informes</span>
            <span className="t-small">Ventas, costos y margen del período</span>
          </div>
          <div className="inf-filters">
            <span className="t-smallbold">Desde</span>
            <DateSpinner value={desde} onChange={setDesde} style={{ width: 118 }} />
            <span className="t-smallbold">Hasta</span>
            <DateSpinner value={hasta} onChange={setHasta} style={{ width: 118 }} />
            <span className="t-smallbold">Tipo</span>
            <Combo value={tipo} options={TIPOS} onChange={setTipo} h={36} style={{ width: 120 }} />
            <span className="t-smallbold">Gan. mín.</span>
            <Field value={ganMin} onChange={setGanMin} h={36} style={{ width: 90 }} onEnter={() => buscar()} title="Solo días con ganancia ≥ este monto (vacío = sin filtro)" />
            <Btn kind="primary" onClick={() => buscar()}>Aplicar</Btn>
            <div style={{ display: 'flex', gap: 6 }}>
              <Btn kind="nav" onClick={() => rango(hoy(), hoy())}>Hoy</Btn>
              <Btn kind="nav" onClick={() => rango(haceDias(6), hoy())}>7 días</Btn>
              <Btn kind="nav" onClick={() => rango(haceDias(29), hoy())}>30 días</Btn>
              <Btn kind="nav" onClick={() => rango(inicioMes(), hoy())}>Mes</Btn>
              <Btn kind="nav" onClick={() => rango(new Date(2000, 0, 1), hoy())}>Todo</Btn>
            </div>
          </div>
        </div>
        <div className="inf-kpis">
          <Kpi title="VENTAS" value={`$${money(r.sumVentas)}`} color="var(--ink)" />
          <Kpi title="COSTO" value={`$${money(r.sumCosto)}`} color="var(--muted)" />
          <Kpi title="GANANCIA" value={`$${money(r.ganancia)}`} color={r.ganancia >= 0 ? 'var(--success)' : 'var(--accent)'} />
          <Kpi title="MARGEN" value={`${Math.round(r.margen)}%`} color="var(--accent)" />
        </div>
      </div>

      <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div className="inf-charts">
          <DonutChart title="Ventas por tipo" slices={r.slicesTipo} caption="Total" value={`$${money(r.sumVentas)}`} />
          <DonutChart title="Resultado del período" slices={r.slicesRes} caption="Margen" value={`${Math.round(r.margen)}%`} />
        </div>
        <div className="inf-detail">
          <div className="t-heading">Detalle diario</div>
          <div className="dark-scroll dark-fill">
            <table className="dark-table">
              <thead>
                <tr><th>Fecha</th><th>Ventas</th><th>Costo</th><th>Ganancia</th><th>Margen</th></tr>
              </thead>
              <tbody>
                {r.filas.map((f, i) => (
                  <tr key={f[0]} className={sel === i ? 'sel' : ''} onMouseDown={() => setSel(i)}>
                    <td>{f[0]}</td>
                    <td className="r">{f[1]}</td>
                    <td className="r">{f[2]}</td>
                    <td className="r">{f[3]}</td>
                    <td className="r">{f[4]}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div className="dlg-footer">
        <span className="t-small">{r.hint}</span>
        <div className="dlg-actions">
          <Btn onClick={exportar}>Exportar Excel</Btn>
          <Btn onClick={vista}>Vista para imprimir</Btn>
          <Btn kind="primary" onClick={onClose}>Cerrar</Btn>
        </div>
      </div>
    </div>
  );
}

function Kpi({ title, value, color }) {
  return (
    <div className="kpi">
      <div className="t-smallbold">{title}</div>
      <div className="kpi-value" style={{ color }}>{value}</div>
    </div>
  );
}
