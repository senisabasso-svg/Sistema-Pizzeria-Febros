import { useSyncExternalStore } from 'react';

function mulberry32(seed) {
  let a = seed >>> 0;
  return function () {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

const rnd = mulberry32(42);
const randint = (a, b) => a + Math.floor(rnd() * (b - a + 1));
const uniform = (a, b) => a + rnd() * (b - a);
const choice = (arr) => arr[Math.floor(rnd() * arr.length)];
function gauss(mu, sigma) {
  const u = 1 - rnd();
  const v = rnd();
  return mu + sigma * Math.sqrt(-2 * Math.log(u)) * Math.cos(2 * Math.PI * v);
}

export const TIPOS_PRODUCTO = ['Pan', 'Facturas', 'Tortas', 'Masas', 'Sandwiches', 'Bebidas', 'Fiambres', 'Otro'];

const BASES = {
  Pan: [
    'Pan francés', 'Pan flauta', 'Pan de campo', 'Pan integral', 'Pan de centeno',
    'Pan de molde', 'Pan de avena', 'Pan ciabatta', 'Pan baguette', 'Pan de semillas',
    'Pan lactal', 'Pan negro', 'Pan de papa', 'Pan hamburguesa', 'Pan hot dog',
    'Pan árabe', 'Pan pita', 'Focaccia', 'Chapata', 'Pan de maíz',
  ],
  Facturas: [
    'Medialuna manteca', 'Medialuna grasa', 'Croissant', 'Vigilante', 'Cañoncito',
    'Sacramento', 'Librito', 'Bola de fraile', 'Churro', 'Tortita negra',
    'Palmera', 'Rosquita', 'Caracola', 'Factura crema', 'Factura dulce de leche',
    'Factura membrillo', 'Factura batata', 'Donut glaseado', 'Donut chocolate', 'Berlinesa',
  ],
  Tortas: [
    'Torta chocolate', 'Torta vainilla', 'Torta tres leches', 'Torta selva negra',
    'Torta lemon pie', 'Torta cheesecake', 'Torta red velvet', 'Torta zanahoria',
    'Torta manzana', 'Torta coco', 'Torta Rogel', 'Torta Balcarce', 'Torta mousse',
    'Torta Oreo', 'Torta frutal', 'Torta cumpleaños', 'Torta naranja', 'Torta café',
    'Torta Nutella', 'Porción chocotorta',
  ],
  Masas: [
    'Alfajor maicena', 'Alfajor chocolate', 'Alfajor cordobés', 'Pepas', 'Galletitas avena',
    'Cookie chips', 'Brownie', 'Budín limón', 'Budín banana', 'Budín marmolado',
    'Muffin arándanos', 'Muffin chocolate', 'Magdalena', 'Bizcochuelo', 'Pasta frola',
    'Tarta coco', 'Tarta ricota', 'Lengua de gato', 'Palitos salados', 'Grisines',
  ],
  Sandwiches: [
    'Sándwich jamón queso', 'Sándwich milanesa', 'Sándwich pollo', 'Sándwich atún',
    'Sándwich vegetariano', 'Sándwich triple', 'Sándwich bondiola', 'Sándwich salame',
    'Tostado JYQ', 'Wrap pollo', 'Wrap vegetales', 'Empanada carne', 'Empanada pollo',
    'Empanada jamón queso', 'Empanada verdura', 'Empanada humita', 'Chipá', 'Pebete',
    'Sandwich de miga', 'Sandwich de miga especial',
  ],
  Bebidas: [
    'Café espresso', 'Café con leche', 'Cortado', 'Capuccino', 'Submarino',
    'Té', 'Mate cocido', 'Chocolate caliente', 'Jugo naranja', 'Limonada',
    'Agua 500ml', 'Agua con gas', 'Coca Cola 500ml', 'Sprite 500ml', 'Fanta 500ml',
    'Cerveza artesanal', 'Licuado banana', 'Licuado frutilla', 'Smoothie mango', 'Agua saborizada',
  ],
  Fiambres: [
    'Jamón cocido 100g', 'Jamón crudo 100g', 'Queso tybo 100g', 'Queso danbo 100g',
    'Salame 100g', 'Mortadela 100g', 'Panceta 100g', 'Bondiola 100g',
    'Queso rallado', 'Manteca 200g', 'Crema chantilly', 'Dulce de leche pote',
    'Membrillo', 'Batata en lata', 'Aceitunas', 'Pickles', 'Huevos docena',
    'Leche 1L', 'Yogur natural', 'Queso crema',
  ],
  Otro: [
    'Bolsa pan', 'Caja torta', 'Servilletas', 'Cucharitas', 'Vaso descartable',
    'Combo desayuno', 'Combo merienda', 'Promo 6 facturas', 'Promo 12 facturas',
    'Promo medialunas x6', 'Promo medialunas x12', 'Bandeja surtida chica',
    'Bandeja surtida grande', 'Encargo especial', 'Pedido catering',
    'Tostadas pack', 'Pan rallado', 'Harina 1kg', 'Azúcar 1kg', 'Levadura',
  ],
};

const VARIANTES = [' clásico', ' especial', ' premium', ' casero', ' del día', ' grande', ' chico', ' x2', ' familiar', ' light', ' integral'];

const RANGO_PRECIO = {
  Pan: [40, 180], Facturas: [35, 120], Tortas: [450, 2200], Masas: [50, 280],
  Sandwiches: [120, 420], Bebidas: [60, 220], Fiambres: [70, 350], Otro: [20, 800],
};

const PROVEEDORES = [
  ['Molinos del Sur', '099111222', 'Harinas y granos'],
  ['Lácteos Uruguay', '099333444', 'Lácteos'],
  ['Distribuidora Dulce', '099555666', 'General'],
  ['Frigorífico Central', '099777888', 'Fiambres'],
  ['Bebidas del Este', '099999000', 'Bebidas'],
  ['Packaging Pro', '091222333', 'Packaging'],
  ['Huevos La Granja', '091444555', 'Huevos'],
  ['Frutas Frescas SA', '091666777', 'General'],
];

const STOCK_ITEMS = [
  ['Harina 000', 'kg', 120, 45, 20, 'Ingrediente'],
  ['Harina 0000', 'kg', 80, 55, 15, 'Ingrediente'],
  ['Harina integral', 'kg', 40, 60, 10, 'Ingrediente'],
  ['Azúcar', 'kg', 90, 38, 15, 'Ingrediente'],
  ['Levadura fresca', 'kg', 2.5, 180, 3, 'Ingrediente'],
  ['Manteca', 'kg', 35, 220, 8, 'Ingrediente'],
  ['Huevos', 'u', 400, 8, 60, 'Ingrediente'],
  ['Leche', 'L', 60, 42, 12, 'Ingrediente'],
  ['Dulce de leche', 'kg', 25, 190, 5, 'Ingrediente'],
  ['Chocolate cobertura', 'kg', 18, 350, 4, 'Ingrediente'],
  ['Sal', 'kg', 30, 18, 5, 'Ingrediente'],
  ['Aceite', 'L', 40, 95, 8, 'Ingrediente'],
  ['Café grano', 'kg', 15, 420, 3, 'Ingrediente'],
  ['Té surtido', 'caja', 20, 160, 4, 'Ingrediente'],
  ['Jamón cocido', 'kg', 10, 480, 2, 'Ingrediente'],
  ['Queso tybo', 'kg', 12, 390, 2, 'Ingrediente'],
  ['Bolsa pan', 'u', 800, 2, 100, 'Packaging'],
  ['Caja torta', 'u', 14, 25, 20, 'Packaging'],
  ['Vaso 8oz', 'u', 500, 3, 80, 'Packaging'],
  ['Servilletas', 'paq', 40, 45, 5, 'Packaging'],
  ['Coca Cola 500', 'u', 96, 55, 24, 'Producto terminado'],
  ['Agua 500', 'u', 120, 28, 24, 'Producto terminado'],
  ['Pan francés listo', 'u', 80, 25, 20, 'Producto terminado'],
  ['Medialuna lista', 'u', 150, 28, 30, 'Producto terminado'],
];

const pad = (n) => String(n).padStart(2, '0');
export const dayKey = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
export const fmtDateTime = (d) => `${dayKey(d)} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
export const fmtTime = (d) => `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;

function buildProducts() {
  const products = [];
  for (const tipo of TIPOS_PRODUCTO) {
    for (const base of BASES[tipo]) {
      const [a, b] = RANGO_PRECIO[tipo];
      const precio = randint(a, b);
      const costo = Math.round(precio * (1 - uniform(0.35, 0.65)));
      products.push({ nombre: base, precio, costo, tipo });
    }
  }
  while (products.length < 200) {
    const tipo = choice(TIPOS_PRODUCTO);
    let nombre = (choice(BASES[tipo]) + choice(VARIANTES)).trim();
    if (products.some((p) => p.nombre === nombre)) nombre = `${nombre} #${products.length + 1}`;
    const [a, b] = RANGO_PRECIO[tipo];
    const precio = randint(a, Math.round(b * 1.1));
    const costo = Math.round(precio * uniform(0.35, 0.65));
    products.push({ nombre, precio, costo, tipo });
  }
  return products.map((p, i) => ({ id: i + 1, ...p }));
}

function buildDb() {
  const productos = buildProducts();
  const proveedores = PROVEEDORES.map(([nombre, telefono, rubro], i) => ({
    id: i + 1, nombre, telefono, rubro, nota: 'Proveedor habitual',
  }));
  const stock = STOCK_ITEMS.map(([nombre, unidad, cantidad, costo, minimo, categoria], i) => ({
    id: i + 1, nombre, unidad, cantidad, costo, minimo, categoria,
  }));

  // ventas[day] = { [tipo]: { ventas, costo } }
  const ventas = {};
  const compras = [];
  const mermas = [];
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const start = new Date(today);
  start.setDate(start.getDate() - 365);
  let cajaId = 0;

  for (let day = new Date(start); day <= today; day.setDate(day.getDate() + 1)) {
    const isToday = day.getTime() === today.getTime();
    if (!isToday && rnd() < 0.06) continue;
    cajaId++;
    const weekday = (day.getDay() + 6) % 7; // 0 = lunes
    let base = weekday < 5 ? 18 : 28;
    if (weekday === 6) base = 22;
    let tickets = Math.max(8, Math.floor(gauss(base, 5)));
    if ([11, 0, 6].includes(day.getMonth())) tickets = Math.floor(tickets * 1.25);
    const ageRatio = (day - start) / (365 * 86400000);
    const key = dayKey(day);
    const porTipo = {};

    for (let t = 0; t < tickets; t++) {
      const hour = choice([7, 8, 9, 10, 11, 12, 16, 17, 18, 19]);
      if (isToday && hour > now.getHours()) continue;
      const items = randint(1, 4);
      for (let i = 0; i < items; i++) {
        const p = choice(productos);
        const factor = 0.92 + 0.16 * ageRatio + uniform(-0.03, 0.03);
        const pu = Math.max(10, Math.round(p.precio * factor));
        const cu = Math.max(1, Math.round(p.costo * factor));
        let cant = p.tipo === 'Tortas' ? 1 : randint(1, 4);
        if (p.tipo === 'Facturas') cant = randint(1, 6);
        const acc = porTipo[p.tipo] || (porTipo[p.tipo] = { ventas: 0, costo: 0 });
        acc.ventas += pu * cant;
        acc.costo += cu * cant;
      }
    }
    if (Object.keys(porTipo).length) ventas[key] = porTipo;

    if (!isToday && (weekday === 1 || weekday === 4) && rnd() < 0.85) {
      const prov = choice(proveedores);
      let total = 0;
      for (let i = 0, n = randint(2, 5); i < n; i++) {
        const s = choice(stock);
        total += Math.round(choice([5, 10, 15, 20, 25, 50]) * s.costo * uniform(0.95, 1.1));
      }
      const fecha = new Date(day);
      fecha.setHours(11, 30, 0);
      compras.push({ id: compras.length + 1, fecha, proveedor: prov.nombre, total, nota: 'Repos semanal' });
    }

    if (!isToday && rnd() < 0.75) {
      const p = choice(productos);
      const cant = randint(1, 8);
      const fecha = new Date(day);
      fecha.setHours(20, 30, 0);
      mermas.push({
        id: mermas.length + 1, fecha, item: p.nombre, cantidad: cant, unidad: 'u',
        motivo: choice(['Sobrante del día', 'Quemado / fallado', 'Vencido', 'Rotura']),
        costo: Math.round(p.costo * cant),
      });
    }
  }

  const apertura = new Date(today);
  apertura.setHours(7, 12, 4);
  const movimientos = [];
  const addMov = (h, m, tipo, monto, concepto) => {
    const f = new Date(today);
    f.setHours(h, m, randint(0, 59));
    if (f <= now) movimientos.push({ id: movimientos.length + 1, fecha: f, tipo, monto, concepto });
  };
  addMov(8, 40, 'ENTRADA', 500, 'Seña torta cumpleaños');
  addMov(10, 15, 'SALIDA', 320, 'Artículos de limpieza');
  addMov(11, 30, 'SALIDA', 1850, 'Pago Lácteos Uruguay');
  addMov(17, 5, 'ENTRADA', 1200, 'Seña pedido catering');

  return {
    comercio: {
      nombre: 'Comercio ejemplo',
      impresora: '(Automática)',
      logo: true,
    },
    productos,
    proveedores,
    stock,
    ventas,
    compras,
    mermas,
    caja: { id: cajaId, apertura: 2000, fechaApertura: apertura, movimientos },
    ultimaCaja: cajaId,
    seq: { producto: productos.length, proveedor: proveedores.length, stock: stock.length },
  };
}

export const db = buildDb();

let version = 0;
const listeners = new Set();

export function commit() {
  version++;
  listeners.forEach((l) => l());
}

export function useDb() {
  useSyncExternalStore(
    (l) => {
      listeners.add(l);
      return () => listeners.delete(l);
    },
    () => version,
  );
  return db;
}

export function registrarVenta(items) {
  const key = dayKey(new Date());
  const porTipo = db.ventas[key] || (db.ventas[key] = {});
  for (const it of items) {
    const p = db.productos.find((x) => x.nombre === it.nombre);
    const tipo = p ? p.tipo : 'Otro';
    const costoUnit = p ? p.costo : 0;
    const acc = porTipo[tipo] || (porTipo[tipo] = { ventas: 0, costo: 0 });
    acc.ventas += it.precio * it.cantidad;
    acc.costo += costoUnit * it.cantidad;
    const s = db.stock.find((x) => x.nombre.toLowerCase() === it.nombre.toLowerCase());
    if (s) s.cantidad = Math.max(0, s.cantidad - it.cantidad);
  }
  commit();
}

export function ventasDelDia(day) {
  const porTipo = db.ventas[dayKey(day)];
  if (!porTipo) return 0;
  return Object.values(porTipo).reduce((a, v) => a + v.ventas, 0);
}

export function parseNum(texto) {
  if (texto == null || String(texto).trim() === '') return null;
  const n = Number(String(texto).replace(',', '.').replace('$', '').trim());
  return Number.isFinite(n) ? n : null;
}

export function formatNum(n) {
  if (Math.abs(n - Math.round(n)) < 0.001) return String(Math.round(n));
  return n.toFixed(2);
}

export const money = (n) => String(Math.round(n));
