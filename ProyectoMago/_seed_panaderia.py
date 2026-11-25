# -*- coding: utf-8 -*-
"""Seed dist/prueba.db as a bakery used for ~1 year: ~200 products + daily sales."""
import random
import sqlite3
from datetime import datetime, timedelta
from pathlib import Path

DB = Path(r"c:\Users\senis\proyectoPizzeria\ProyectoMago\dist\prueba.db")
rng = random.Random(42)

TIPOS = {
    "Pan": [
        "Pan francés", "Pan flauta", "Pan de campo", "Pan integral", "Pan de centeno",
        "Pan de molde", "Pan de avena", "Pan ciabatta", "Pan baguette", "Pan de semillas",
        "Pan lactal", "Pan negro", "Pan de papa", "Pan hamburguesa", "Pan hot dog",
        "Pan árabe", "Pan pita", "Focaccia", "Chapata", "Pan de maíz",
    ],
    "Facturas": [
        "Medialuna manteca", "Medialuna grasa", "Croissant", "Vigilante", "Cañoncito",
        "Sacramento", "Librito", "Bola de fraile", "Churro", "Tortita negra",
        "Palmera", "Rosquita", "Caracola", "Factura crema", "Factura dulce de leche",
        "Factura membrillo", "Factura batata", "Donut glaseado", "Donut chocolate", "Berlinesa",
    ],
    "Tortas": [
        "Torta chocolate", "Torta vainilla", "Torta tres leches", "Torta selva negra",
        "Torta lemon pie", "Torta cheesecake", "Torta red velvet", "Torta zanahoria",
        "Torta manzana", "Torta coco", "Torta Rogel", "Torta Balcarce", "Torta mousse",
        "Torta Oreo", "Torta frutal", "Torta cumpleaños", "Torta naranja", "Torta café",
        "Torta Nutella", "Porción chocotorta",
    ],
    "Masas": [
        "Alfajor maicena", "Alfajor chocolate", "Alfajor cordobés", "Pepas", "Galletitas avena",
        "Cookie chips", "Brownie", "Budín limón", "Budín banana", "Budín marmolado",
        "Muffin arándanos", "Muffin chocolate", "Magdalena", "Bizcochuelo", "Pasta frola",
        "Tarta coco", "Tarta ricota", "Lengua de gato", "Palitos salados", "Grisines",
    ],
    "Sandwiches": [
        "Sándwich jamón queso", "Sándwich milanesa", "Sándwich pollo", "Sándwich atún",
        "Sándwich vegetariano", "Sándwich triple", "Sándwich bondiola", "Sándwich salame",
        "Tostado JYQ", "Wrap pollo", "Wrap vegetales", "Empanada carne", "Empanada pollo",
        "Empanada jamón queso", "Empanada verdura", "Empanada humita", "Chipá", "Pebete",
        "Sandwich de miga", "Sandwich de miga especial",
    ],
    "Bebidas": [
        "Café espresso", "Café con leche", "Cortado", "Capuccino", "Submarino",
        "Té", "Mate cocido", "Chocolate caliente", "Jugo naranja", "Limonada",
        "Agua 500ml", "Agua con gas", "Coca Cola 500ml", "Sprite 500ml", "Fanta 500ml",
        "Cerveza artesanal", "Licuado banana", "Licuado frutilla", "Smoothie mango", "Agua saborizada",
    ],
    "Fiambres": [
        "Jamón cocido 100g", "Jamón crudo 100g", "Queso tybo 100g", "Queso danbo 100g",
        "Salame 100g", "Mortadela 100g", "Panceta 100g", "Bondiola 100g",
        "Queso rallado", "Manteca 200g", "Crema chantilly", "Dulce de leche pote",
        "Membrillo", "Batata en lata", "Aceitunas", "Pickles", "Huevos docena",
        "Leche 1L", "Yogur natural", "Queso crema",
    ],
    "Otro": [
        "Bolsa pan", "Caja torta", "Servilletas", "Cucharitas", "Vaso descartable",
        "Combo desayuno", "Combo merienda", "Promo 6 facturas", "Promo 12 facturas",
        "Promo medialunas x6", "Promo medialunas x12", "Bandeja surtida chica",
        "Bandeja surtida grande", "Encargo especial", "Pedido catering",
        "Tostadas pack", "Pan rallado", "Harina 1kg", "Azúcar 1kg", "Levadura",
    ],
}

VARIANTES = [
    "", " clásico", " especial", " premium", " casero", " del día",
    " grande", " chico", " x2", " familiar", " light", " integral",
]

PROVEEDORES = [
    ("Molinos del Sur", "099111222", "Harinas"),
    ("Lácteos Uruguay", "099333444", "Lácteos"),
    ("Distribuidora Dulce", "099555666", "Insumos"),
    ("Frigorífico Central", "099777888", "Fiambres"),
    ("Bebidas del Este", "099999000", "Bebidas"),
    ("Packaging Pro", "091222333", "Packaging"),
    ("Huevos La Granja", "091444555", "Huevos"),
    ("Frutas Frescas SA", "091666777", "Frutas"),
]

STOCK_ITEMS = [
    ("Harina 000", "kg", 120, 45, 20, "Ingrediente"),
    ("Harina 0000", "kg", 80, 55, 15, "Ingrediente"),
    ("Harina integral", "kg", 40, 60, 10, "Ingrediente"),
    ("Azúcar", "kg", 90, 38, 15, "Ingrediente"),
    ("Levadura fresca", "kg", 12, 180, 3, "Ingrediente"),
    ("Manteca", "kg", 35, 220, 8, "Ingrediente"),
    ("Huevos", "u", 400, 8, 60, "Ingrediente"),
    ("Leche", "L", 60, 42, 12, "Ingrediente"),
    ("Dulce de leche", "kg", 25, 190, 5, "Ingrediente"),
    ("Chocolate cobertura", "kg", 18, 350, 4, "Ingrediente"),
    ("Sal", "kg", 30, 18, 5, "Ingrediente"),
    ("Aceite", "L", 40, 95, 8, "Ingrediente"),
    ("Bolsa pan", "u", 800, 2, 100, "Packaging"),
    ("Caja torta", "u", 120, 25, 20, "Packaging"),
    ("Vaso 8oz", "u", 500, 3, 80, "Packaging"),
    ("Servilletas", "paq", 40, 45, 5, "Packaging"),
    ("Café grano", "kg", 15, 420, 3, "Ingrediente"),
    ("Té surtido", "caja", 20, 160, 4, "Ingrediente"),
    ("Jamón cocido", "kg", 10, 480, 2, "Ingrediente"),
    ("Queso tybo", "kg", 12, 390, 2, "Ingrediente"),
    ("Coca Cola 500", "u", 96, 55, 24, "Producto"),
    ("Agua 500", "u", 120, 28, 24, "Producto"),
    ("Pan francés listo", "u", 80, 25, 20, "Producto"),
    ("Medialuna lista", "u", 150, 28, 30, "Producto"),
]


def build_products(target=200):
    products = []
    for tipo, bases in TIPOS.items():
        for base in bases:
            precio = {
                "Pan": rng.randint(40, 180),
                "Facturas": rng.randint(35, 120),
                "Tortas": rng.randint(450, 2200),
                "Masas": rng.randint(50, 280),
                "Sandwiches": rng.randint(120, 420),
                "Bebidas": rng.randint(60, 220),
                "Fiambres": rng.randint(70, 350),
                "Otro": rng.randint(20, 800),
            }[tipo]
            margen = rng.uniform(0.35, 0.65)
            costo = round(precio * (1 - margen), 0)
            products.append((base, float(precio), float(costo), tipo))

    # Expand to ~200 with variants
    while len(products) < target:
        tipo = rng.choice(list(TIPOS.keys()))
        base = rng.choice(TIPOS[tipo])
        var = rng.choice(VARIANTES[1:])
        nombre = f"{base}{var}".strip()
        if any(p[0] == nombre for p in products):
            nombre = f"{nombre} #{len(products)+1}"
        precio = {
            "Pan": rng.randint(40, 200),
            "Facturas": rng.randint(35, 140),
            "Tortas": rng.randint(450, 2500),
            "Masas": rng.randint(50, 300),
            "Sandwiches": rng.randint(120, 450),
            "Bebidas": rng.randint(60, 250),
            "Fiambres": rng.randint(70, 380),
            "Otro": rng.randint(20, 900),
        }[tipo]
        costo = round(precio * rng.uniform(0.35, 0.65), 0)
        products.append((nombre, float(precio), float(costo), tipo))

    return products[:target]


def main():
    if not DB.exists():
        raise SystemExit(f"No existe DB: {DB}")

    products = build_products(200)
    conn = sqlite3.connect(str(DB), timeout=30)
    conn.execute("PRAGMA journal_mode=WAL")
    cur = conn.cursor()

    # Wipe transactional/demo tables but keep schema
    for t in [
        "venta_detalle", "informes", "movimientos_caja", "cajas",
        "compra_detalle", "compras", "mermas", "stock", "proveedores", "menuPrecios",
    ]:
        cur.execute(f"DELETE FROM {t}")

    # Proveedores
    prov_ids = []
    for nombre, tel, rubro in PROVEEDORES:
        cur.execute(
            "INSERT INTO proveedores (nombre, telefono, rubro, nota) VALUES (?,?,?,?)",
            (nombre, tel, rubro, "Proveedor habitual"),
        )
        prov_ids.append(cur.lastrowid)

    # Stock
    stock_ids = []
    for nombre, unidad, cant, costo, minimo, cat in STOCK_ITEMS:
        pid = rng.choice(prov_ids)
        cur.execute(
            "INSERT INTO stock (nombre, cantidad, unidad, costo_unitario, minimo, categoria, proveedor_id) "
            "VALUES (?,?,?,?,?,?,?)",
            (nombre, cant, unidad, costo, minimo, cat, pid),
        )
        stock_ids.append((cur.lastrowid, nombre, unidad, costo))

    # Productos
    for nombre, precio, costo, tipo in products:
        cur.execute(
            "INSERT INTO menuPrecios (nombre, precio, costo, tipo) VALUES (?,?,?,?)",
            (nombre, precio, costo, tipo),
        )

    # Year of activity
    today = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
    start = today - timedelta(days=365)

    day = start
    ventas_rows = 0
    informes_rows = 0
    cajas_rows = 0
    compras_rows = 0
    mermas_rows = 0

    while day <= today:
        # Closed Mondays sometimes (panadería abierta casi siempre, cerramos ~8% días)
        if rng.random() < 0.06:
            day += timedelta(days=1)
            continue

        # Weekend busier
        weekday = day.weekday()  # 0=Mon
        base_tickets = 18 if weekday < 5 else 28
        if weekday == 6:  # Sunday
            base_tickets = 22
        n_tickets = max(8, int(rng.gauss(base_tickets, 5)))

        # Seasonal bump Dec/Jan and July
        if day.month in (12, 1, 7):
            n_tickets = int(n_tickets * 1.25)

        day_total = 0.0
        day_cost = 0.0

        # Open/close caja
        apertura = 1500 + rng.randint(0, 1000)
        caja_fecha_a = day.replace(hour=7, minute=rng.randint(0, 40))
        caja_fecha_c = day.replace(hour=20, minute=rng.randint(0, 50))
        cur.execute(
            "INSERT INTO cajas (fecha_apertura, monto_apertura, fecha_cierre, monto_esperado, "
            "monto_contado, diferencia, estado, nota) VALUES (?,?,?,?,?,?,?,?)",
            (
                caja_fecha_a.strftime("%Y-%m-%d %H:%M:%S"),
                float(apertura),
                caja_fecha_c.strftime("%Y-%m-%d %H:%M:%S"),
                0.0, 0.0, 0.0, "CERRADA", "Caja del día",
            ),
        )
        caja_id = cur.lastrowid
        cajas_rows += 1

        for _ in range(n_tickets):
            hour = rng.choice([7, 8, 9, 10, 11, 12, 16, 17, 18, 19])
            minute = rng.randint(0, 59)
            second = rng.randint(0, 59)
            fecha = day.replace(hour=hour, minute=minute, second=second)
            fecha_s = fecha.strftime("%Y-%m-%d %H:%M:%S")

            items = rng.randint(1, 4)
            ticket_sub = 0.0
            for _i in range(items):
                nombre, precio, costo, tipo = rng.choice(products)
                # Slight price drift over the year (+/- 10%)
                age_ratio = (day - start).days / 365.0
                factor = 0.92 + 0.16 * age_ratio + rng.uniform(-0.03, 0.03)
                p = max(10, round(precio * factor))
                c = max(1, round(costo * factor))
                cant = 1 if tipo in ("Tortas",) else rng.randint(1, 4)
                if tipo == "Facturas":
                    cant = rng.randint(1, 6)
                sub = p * cant
                costo_t = c * cant
                cur.execute(
                    "INSERT INTO venta_detalle (fecha, producto, tipo, cantidad, precio_unit, "
                    "costo_unit, subtotal, costo_total) VALUES (?,?,?,?,?,?,?,?)",
                    (fecha_s, nombre, tipo, cant, float(p), float(c), float(sub), float(costo_t)),
                )
                ventas_rows += 1
                ticket_sub += sub
                day_total += sub
                day_cost += costo_t

            # movimiento entrada por ticket (agrupado a veces)
            if rng.random() < 0.35:
                cur.execute(
                    "INSERT INTO movimientos_caja (caja_id, fecha, tipo, monto, concepto) VALUES (?,?,?,?,?)",
                    (caja_id, fecha_s, "ENTRADA", float(ticket_sub), "Venta mostrador"),
                )

        # Daily informe
        informe_fecha = day.replace(hour=21, minute=5, second=0)
        cur.execute(
            "INSERT INTO informes (fecha, fechaActivo, total) VALUES (?,?,?)",
            (
                informe_fecha.strftime("%Y-%m-%d %H:%M:%S"),
                informe_fecha.strftime("%Y-%m-%d"),
                float(round(day_total, 0)),
            ),
        )
        informes_rows += 1

        # Update caja expected/counted
        esperado = apertura + day_total
        contado = esperado + rng.randint(-80, 80)
        cur.execute(
            "UPDATE cajas SET monto_esperado=?, monto_contado=?, diferencia=? WHERE id=?",
            (float(esperado), float(contado), float(contado - esperado), caja_id),
        )

        # Occasional purchase (~2 per week)
        if weekday in (1, 4) and rng.random() < 0.85:
            idx = rng.randrange(len(PROVEEDORES))
            pid = prov_ids[idx]
            pname = PROVEEDORES[idx][0]
            total_compra = 0.0
            cur.execute(
                "INSERT INTO compras (proveedor_id, proveedor_nombre, fecha, total, nota) VALUES (?,?,?,?,?)",
                (pid, pname, day.strftime("%Y-%m-%d %H:%M:%S"), 0.0, "Repos semanal"),
            )
            compra_id = cur.lastrowid
            for _ in range(rng.randint(2, 5)):
                sid, sname, sunidad, scosto = rng.choice(stock_ids)
                cant = rng.choice([5, 10, 15, 20, 25, 50])
                cu = scosto * rng.uniform(0.95, 1.1)
                sub = round(cant * cu, 0)
                total_compra += sub
                cur.execute(
                    "INSERT INTO compra_detalle (compra_id, stock_id, item_nombre, cantidad, "
                    "costo_unit, subtotal, unidad) VALUES (?,?,?,?,?,?,?)",
                    (compra_id, sid, sname, float(cant), float(round(cu, 0)), float(sub), sunidad),
                )
            cur.execute("UPDATE compras SET total=? WHERE id=?", (float(total_compra), compra_id))
            compras_rows += 1
            cur.execute(
                "INSERT INTO movimientos_caja (caja_id, fecha, tipo, monto, concepto) VALUES (?,?,?,?,?)",
                (
                    caja_id,
                    day.replace(hour=11, minute=30).strftime("%Y-%m-%d %H:%M:%S"),
                    "SALIDA",
                    float(total_compra),
                    f"Compra {pname}",
                ),
            )

        # Merma almost every day
        if rng.random() < 0.75:
            nombre, precio, costo, tipo = rng.choice(products)
            cant = rng.randint(1, 8)
            cur.execute(
                "INSERT INTO mermas (fecha, item_nombre, cantidad, unidad, motivo, costo_estimado) "
                "VALUES (?,?,?,?,?,?)",
                (
                    day.replace(hour=20, minute=30).strftime("%Y-%m-%d %H:%M:%S"),
                    nombre,
                    float(cant),
                    "u",
                    rng.choice(["Sobrante del día", "Quemado", "Vencido", "Rotura"]),
                    float(round(costo * cant, 0)),
                ),
            )
            mermas_rows += 1

        day += timedelta(days=1)

    conn.commit()

    summary = {
        "productos": cur.execute("SELECT COUNT(*) FROM menuPrecios").fetchone()[0],
        "ventas_lineas": cur.execute("SELECT COUNT(*) FROM venta_detalle").fetchone()[0],
        "informes": cur.execute("SELECT COUNT(*) FROM informes").fetchone()[0],
        "cajas": cur.execute("SELECT COUNT(*) FROM cajas").fetchone()[0],
        "proveedores": cur.execute("SELECT COUNT(*) FROM proveedores").fetchone()[0],
        "stock": cur.execute("SELECT COUNT(*) FROM stock").fetchone()[0],
        "compras": cur.execute("SELECT COUNT(*) FROM compras").fetchone()[0],
        "mermas": cur.execute("SELECT COUNT(*) FROM mermas").fetchone()[0],
        "venta_min": cur.execute("SELECT MIN(fecha) FROM venta_detalle").fetchone()[0],
        "venta_max": cur.execute("SELECT MAX(fecha) FROM venta_detalle").fetchone()[0],
        "total_ventas": cur.execute("SELECT ROUND(SUM(subtotal),0) FROM venta_detalle").fetchone()[0],
    }
    conn.close()
    print("DB", DB)
    for k, v in summary.items():
        print(f"{k}: {v}")
    print("SEED_OK")


if __name__ == "__main__":
    main()
