import { createContext, useCallback, useContext, useEffect, useLayoutEffect, useRef, useState } from 'react';

/* ---------------- ventanas ---------------- */

export function DialogWindow({ width, height, children }) {
  const stageH = 800;
  const stageW = 1280;
  const w = Math.min(width, stageW - 20);
  const h = Math.min(height, stageH - 16);
  return (
    <div className="modal-layer">
      <div className="win" style={{ width: w, height: h, left: (stageW - w) / 2, top: (stageH - h) / 2 }}>
        <div className="win-body">{children}</div>
      </div>
    </div>
  );
}

/* ---------------- controles ---------------- */

export function Btn({ kind = 'secondary', children, style, onClick, className = '' }) {
  return (
    <button type="button" className={`btn btn-${kind} ${className}`} style={style} onClick={onClick}>
      {children}
    </button>
  );
}

export function Field({ value, onChange, h, type = 'text', style, placeholder, onEnter, title }) {
  return (
    <input
      className={`field ${h ? `h${h}` : ''}`}
      type={type}
      value={value}
      style={style}
      title={title}
      placeholder={placeholder}
      spellCheck={false}
      onChange={(e) => onChange && onChange(e.target.value)}
      onKeyDown={(e) => {
        if (e.key === 'Enter' && onEnter) onEnter();
      }}
    />
  );
}

export function Combo({ value, options, onChange, h, style }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  const popRef = useRef(null);
  const items = options.map((o) => (typeof o === 'string' ? { value: o, label: o } : o));
  const current = items.find((o) => o.value === value);

  useEffect(() => {
    if (!open) return undefined;
    const close = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, [open]);

  useLayoutEffect(() => {
    if (open && popRef.current) {
      const sel = popRef.current.querySelector('.sel');
      if (sel) popRef.current.scrollTop = sel.offsetTop - popRef.current.clientHeight / 2 + sel.clientHeight / 2;
    }
  }, [open]);

  return (
    <div className={`combo ${h ? `h${h}` : ''}`} style={style} ref={ref} onMouseDown={() => setOpen((o) => !o)}>
      <div className="combo-value">{current ? current.label : ''}</div>
      <div className="combo-arrow"><i /></div>
      {open && (
        <div className="combo-popup" ref={popRef} onMouseDown={(e) => e.stopPropagation()}>
          {items.map((o) => (
            <div
              key={o.value}
              className={`combo-item ${o.value === value ? 'sel' : ''}`}
              onMouseUp={() => {
                onChange(o.value);
                setOpen(false);
              }}
            >
              {o.label}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export function NumberSpinner({ value, onChange, min = 1, max = 99, h, style }) {
  const [text, setText] = useState(String(value));
  useEffect(() => setText(String(value)), [value]);
  const set = (n) => onChange(Math.min(max, Math.max(min, n)));
  return (
    <div className={`spinner ${h ? `h${h}` : ''}`} style={style}>
      <input
        value={text}
        onChange={(e) => setText(e.target.value.replace(/[^0-9]/g, ''))}
        onBlur={() => {
          const n = parseInt(text, 10);
          if (Number.isFinite(n)) set(n);
          else setText(String(value));
        }}
      />
      <div className="spin-btns">
        <button className="up" tabIndex={-1} onClick={() => set(value + 1)}><i /></button>
        <button className="down" tabIndex={-1} onClick={() => set(value - 1)}><i /></button>
      </div>
    </div>
  );
}

const pad2 = (n) => String(n).padStart(2, '0');
export const fmtDia = (d) => `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}/${d.getFullYear()}`;

export function DateSpinner({ value, onChange, style }) {
  const [text, setText] = useState(fmtDia(value));
  useEffect(() => setText(fmtDia(value)), [value]);
  const shift = (n) => {
    const d = new Date(value);
    d.setDate(d.getDate() + n);
    onChange(d);
  };
  return (
    <div className="spinner h36" style={style}>
      <input
        value={text}
        onChange={(e) => setText(e.target.value)}
        onBlur={() => {
          const m = text.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
          if (m) onChange(new Date(+m[3], +m[2] - 1, +m[1]));
          else setText(fmtDia(value));
        }}
      />
      <div className="spin-btns">
        <button className="up" tabIndex={-1} onClick={() => shift(1)}><i /></button>
        <button className="down" tabIndex={-1} onClick={() => shift(-1)}><i /></button>
      </div>
    </div>
  );
}

export function SectionCard({ title, children, style, bodyStyle }) {
  return (
    <div className="card" style={style}>
      <div className="card-head">
        <div className="bar" />
        <div className="t-heading">{title}</div>
      </div>
      <div className="card-body" style={bodyStyle}>{children}</div>
    </div>
  );
}

/* ---------------- tablas ---------------- */

export function SwingTable({ columns, rows, selected, onSelect }) {
  return (
    <div className="swing-scroll">
      <table className="swing-table">
        <colgroup>
          {columns.map((c) => <col key={c.title} style={c.width ? { width: c.width } : undefined} />)}
        </colgroup>
        <thead>
          <tr>{columns.map((c) => <th key={c.title}>{c.title}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i} className={selected === i ? 'sel' : ''} onMouseDown={() => onSelect && onSelect(i)}>
              {r.map((cell, j) => <td key={j}>{cell}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/* ---------------- dona ---------------- */

export const PALETTE = ['#FF8A1F', '#3DDC97', '#3D8BFF', '#E07800', '#9AA8BC', '#F7FAFF', '#5B7C8A', '#C4A35A'];
const FONT = "'Segoe UI', system-ui, -apple-system, Arial, sans-serif";

function roundRect(g, x, y, w, h, r) {
  g.beginPath();
  g.moveTo(x + r, y);
  g.arcTo(x + w, y, x + w, y + h, r);
  g.arcTo(x + w, y + h, x, y + h, r);
  g.arcTo(x, y + h, x, y, r);
  g.arcTo(x, y, x + w, y, r);
  g.closePath();
}

export function DonutChart({ title, slices, caption, value }) {
  const wrap = useRef(null);
  const canvas = useRef(null);
  const [size, setSize] = useState({ w: 0, h: 0 });

  useEffect(() => {
    const ro = new ResizeObserver(([e]) => setSize({ w: Math.floor(e.contentRect.width), h: Math.floor(e.contentRect.height) }));
    ro.observe(wrap.current);
    return () => ro.disconnect();
  }, []);

  useEffect(() => {
    const { w, h } = size;
    if (!w || !h) return;
    const dpr = Math.max(2, window.devicePixelRatio || 1);
    const c = canvas.current;
    c.width = w * dpr;
    c.height = h * dpr;
    const g = c.getContext('2d');
    g.setTransform(dpr, 0, 0, dpr, 0, 0);
    g.clearRect(0, 0, w, h);

    roundRect(g, 0.5, 0.5, w - 1, h - 1, 9);
    g.fillStyle = '#12233A';
    g.fill();
    g.lineWidth = 1.2;
    g.strokeStyle = '#24374F';
    g.stroke();
    roundRect(g, 16, 10, 28, 3, 1.5);
    g.fillStyle = '#FF8A1F';
    g.fill();

    const pad = 16;
    g.font = `700 15px ${FONT}`;
    g.fillStyle = '#F7FAFF';
    g.textBaseline = 'alphabetic';
    g.fillText(title, pad, pad + 14);

    const total = slices.reduce((a, s) => a + (s.value > 0 ? s.value : 0), 0);
    const chartTop = pad + 28;
    const legendH = Math.min(90, 18 + slices.length * 18);
    let chartSize = Math.min(w - pad * 2, h - chartTop - legendH - 12);
    chartSize = Math.max(120, chartSize);
    const cx = w / 2;
    const cy = chartTop + chartSize / 2;
    const outer = chartSize / 2;
    const inner = (chartSize * 0.58) / 2;

    if (total <= 0 || !slices.length) {
      g.fillStyle = '#1A2E48';
      g.beginPath(); g.arc(cx, cy, outer, 0, Math.PI * 2); g.fill();
      g.fillStyle = '#12233A';
      g.beginPath(); g.arc(cx, cy, inner, 0, Math.PI * 2); g.fill();
      g.font = `12px ${FONT}`;
      g.fillStyle = '#9AA8BC';
      g.textAlign = 'center';
      g.fillText('Sin datos', cx, cy + 4);
      g.textAlign = 'left';
    } else {
      g.fillStyle = 'rgba(0,0,0,0.07)';
      g.beginPath(); g.arc(cx + 3, cy + 5, outer, 0, Math.PI * 2); g.fill();
      let angle = -Math.PI / 2;
      for (const s of slices) {
        if (s.value <= 0) continue;
        const ext = (s.value / total) * Math.PI * 2;
        g.fillStyle = s.color;
        g.beginPath();
        g.moveTo(cx, cy);
        g.arc(cx, cy, outer, angle, angle + ext);
        g.closePath();
        g.fill();
        angle += ext;
      }
      g.fillStyle = '#12233A';
      g.beginPath(); g.arc(cx, cy, inner, 0, Math.PI * 2); g.fill();
      g.lineWidth = 1.2;
      g.strokeStyle = '#24374F';
      g.stroke();
      g.textAlign = 'center';
      g.font = `12px ${FONT}`;
      g.fillStyle = '#9AA8BC';
      g.fillText(caption, cx, cy - 8);
      g.font = `700 16px ${FONT}`;
      g.fillStyle = '#F7FAFF';
      g.fillText(value, cx, cy + 14);
      g.textAlign = 'left';
    }

    let ly = chartTop + chartSize + 10;
    g.font = `12px ${FONT}`;
    for (const s of slices) {
      if (s.value <= 0) continue;
      if (ly + 16 > h - 8) break;
      roundRect(g, pad, ly, 10, 10, 1.5);
      g.fillStyle = s.color;
      g.fill();
      g.fillStyle = '#F7FAFF';
      const pct = total > 0 ? `${Math.round((s.value * 100) / total)}%` : '';
      g.fillText(`${s.label}  $${Math.round(s.value)}  ${pct}`, pad + 16, ly + 10);
      ly += 18;
    }
  }, [size, slices, title, caption, value]);

  return (
    <div ref={wrap} style={{ position: 'relative', minWidth: 0, minHeight: 0 }}>
      <canvas ref={canvas} style={{ position: 'absolute', inset: 0, width: '100%', height: '100%' }} />
    </div>
  );
}

/* ---------------- JOptionPane ---------------- */

function OpIcon({ type }) {
  if (type === 'plain') return null;
  if (type === 'warning') {
    return (
      <svg className="op-icon" viewBox="0 0 32 32">
        <path d="M16 3 30 28H2z" fill="#ffc83d" stroke="#c99a00" strokeWidth="1" />
        <rect x="14.6" y="11" width="2.8" height="9" fill="#000" />
        <rect x="14.6" y="22.2" width="2.8" height="2.8" fill="#000" />
      </svg>
    );
  }
  if (type === 'error') {
    return (
      <svg className="op-icon" viewBox="0 0 32 32">
        <circle cx="16" cy="16" r="14" fill="#e81123" />
        <path d="M10.5 10.5l11 11M21.5 10.5l-11 11" stroke="#fff" strokeWidth="2.6" />
      </svg>
    );
  }
  return (
    <svg className="op-icon" viewBox="0 0 32 32">
      <circle cx="16" cy="16" r="14" fill="#1e73d9" />
      {type === 'question' ? (
        <text x="16" y="22.5" textAnchor="middle" fontSize="19" fontWeight="700" fill="#fff" fontFamily="Segoe UI, Arial">?</text>
      ) : (
        <>
          <rect x="14.5" y="13" width="3" height="11" fill="#fff" />
          <rect x="14.5" y="7.5" width="3" height="3" fill="#fff" />
        </>
      )}
    </svg>
  );
}

const OptionPaneCtx = createContext(null);
export const useOptionPane = () => useContext(OptionPaneCtx);

function OptionDialog({ d, onDone }) {
  const [text, setText] = useState(d.initial ?? '');
  const inputRef = useRef(null);
  const defRef = useRef(null);
  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    } else if (defRef.current) defRef.current.focus();
  }, []);
  const finish = (i) => onDone(d.kind === 'input' ? (i === 0 ? text : null) : i);
  return (
    <div className="op-backdrop">
      <div
        className="op"
        onKeyDown={(e) => {
          if (e.key === 'Enter') finish(d.defaultIndex ?? 0);
          if (e.key === 'Escape') finish(d.kind === 'input' ? 1 : d.buttons.length - 1);
        }}
      >
        <div className="op-body">
          <OpIcon type={d.type} />
          <div style={{ flex: 1 }}>
            {d.textArea ? <div className="op-text">{d.message}</div> : <div className="op-msg">{d.message}</div>}
            {d.kind === 'input' && (
              <input ref={inputRef} className="op-input" value={text} onChange={(e) => setText(e.target.value)} />
            )}
          </div>
        </div>
        <div className="op-buttons">
          {d.buttons.map((b, i) => (
            <button
              key={b}
              ref={i === (d.defaultIndex ?? 0) ? defRef : undefined}
              className={`op-btn ${i === (d.defaultIndex ?? 0) ? 'def' : ''}`}
              onClick={() => finish(i)}
            >
              {b}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

export function OptionPaneProvider({ children }) {
  const [stack, setStack] = useState([]);
  const open = useCallback(
    (d) =>
      new Promise((resolve) => {
        const id = Math.random();
        setStack((s) => [...s, { ...d, id, resolve }]);
      }),
    [],
  );
  const api = useRef(null);
  if (!api.current) {
    api.current = {
      message: (message, title = 'Mensaje', type = 'info') =>
        open({ kind: 'message', message, title, type, buttons: ['Aceptar'] }),
      confirm: (message, title = 'Seleccione una opción') =>
        open({ kind: 'confirm', message, title, type: 'question', buttons: ['Sí', 'No'] }).then((i) => i === 0),
      input: (message, initial = '', title = 'Entrada') =>
        open({ kind: 'input', message, initial, title, type: 'question', buttons: ['Aceptar', 'Cancelar'] }),
      textArea: (message, title, buttons = ['Aceptar'], defaultIndex = 0) =>
        open({ kind: 'options', message, title, type: 'plain', textArea: true, buttons, defaultIndex }),
    };
  }
  return (
    <OptionPaneCtx.Provider value={api.current}>
      {children}
      {stack.map((d) => (
        <OptionDialog
          key={d.id}
          d={d}
          onDone={(r) => {
            setStack((s) => s.filter((x) => x.id !== d.id));
            d.resolve(r);
          }}
        />
      ))}
    </OptionPaneCtx.Provider>
  );
}
