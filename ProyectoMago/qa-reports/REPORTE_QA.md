# Reporte QA — ProyectoMago (Gestión almacenes)

Suite: `qa-tests` / `AlmacenQaSuite` · 20 corridas (`run_20.py`)  
Corridas 1–10: base limpia · Corridas 11–20: acumulan datos  
Logs: `qa-reports/run_01.log` … `run_20.log`

**Alerta CRÍTICOS:** A1 no es estable en acumulación (FALLA 12–20). A2–A4 pasan 20/20.

---

## Tabla resumen

| ID | Criterio | OK/20 | Veredicto | Evidencia |
|----|----------|-------|-----------|-----------|
| A1 | CANT=3 → 3 uds × precio; pegar/Enter/cobrar | **11/20** | **FLAKY** | PASA 1–11; FALLA 12–20 `subtotal esperado 300, fue 30.0`. Cantidad 3 OK; el 1.er ítem del combo ya no es Agua@$100. `PrincipalPanel.java:558` `ORDER BY nombre`; B4 inserta productos @$10 (`Pan de ñame`, `🙂 Agua`, `AAA…`) que ganan el índice 0 → 3×10=30. `commitEdit`: `PrincipalPanel.java:283-287`. |
| A2 | Compras costo 68→70 sin concatenar | 20/20 | PASA | `ComprasDialog.java:201-203` `selectAll`; tipar/pegar/tab → 70 |
| A3 | Costo +300% confirma; Sí guarda / No no toca | 20/20 | PASA | `ComprasDialog.java:249-258`; cancel = early return. Click UI “No”: parcial vía código (no AssertJ dialog) |
| A4 | Sin stock homónimo: avisa, no bloquea, registra | 20/20 | PASA | `PrincipalPanel.java:390-397,446-460`; venta_detalle insertada + aviso sinStock |
| A5 | +Entrada/−Salida reflejan; Actualizar no revierte | 20/20 | PASA | `StockDialog.java:346+` repoblar; cantidad conserva post-Actualizar |
| A6 | Producto nuevo → stock; no duplica/pisa existente | **11/11*** | **FLAKY** | *RESULT solo en limpio. PASA clean: `ProductosDialog.java:343-365`. Corridas 12–20: excepción `UNIQUE menuPrecios.sku` (`SKU-NEW-1` ya existe) — sin línea RESULT (`run_12.log`). |
| A7 | “Guardar nuevo” 1.click post-combo | 20/20 | PASA† | `hidePopup` en Productos/Proveedores/Stock + 1 invocación OK. †Click AssertJ real post-popup: **NO VERIFICADO** (`IllegalComponentState` sin showing) |
| A8 | Caja ventas del turno, total, sin dup mov | 20/20 | PASA | `CajaDialog.java:277-305`; esperado=apertura+ventas+ent−sal |
| A9 | Sin JOptionPane EN en recursos | 20/20 | PASA† | Sin literales OK/Cancel/Yes/No/Message/Input en `src`; UIManager ES. †Botones nativos OS: depende locale Windows — **NO VERIFICADO** runtime |
| A10 | Campos usables, ancho mín >150px | **0/20** | **FALLA** | Productos: JTextField `pref=128×36` `setPreferredSize=false` (editor combo). Form fields: `minimumSize` 120 (`ProductosDialog.java:437`) / 100 (`StockDialog.java:419`). Medición live: Stock unidad pref≈98; varios pref≈122. Criterio “mín >150” no se cumple. |
| A11 | Leyenda donut lista TODOS los tipos | **0/20** | **FALLA** | Con h=140, `ly=146` → `DonutChartPanel.java:185-187` `break` omite slices. 2 columnas ayudan en h=360; paneles chicos siguen truncando. |
| A12 | Resumen caja no trunca con “.” | 20/20 | PASA | HTML 2 líneas en `lblResumen` (apertura/ventas/esperado) |
| A13 | Tipos/categorías/rubros = una fuente | 20/20 | PASA | `Catalogos.java`; Productos/Stock/Proveedores/Informes |
| B1 | Cant 0, neg, 999999, abc, vacío, 1,5, 1.5 | **0/20** | **FALLA** | `[0→1, -1→1, 999999→1, abc→1, →1, 1,5→1, 1.5→15]`. `Pedido.java:19` `Math.max(1,…)`; `commitEdit` swallow (`PrincipalPanel.java:283-287`). **1.5→15** (concatena dígitos / parse spinner). |
| B2 | Precio/costo 0 y negativos | **0/20** | **FALLA** | `parseMonto("0")` → 0; `guardarNuevo` solo null-check (`ProductosDialog.java:227-229`) — acepta vender a $0. Negativos: parse rechaza (OK parcial). |
| B3 | SKU dup / vacío / 200 chars | **11/11*** | **FLAKY** | Clean: dup UI+index; vacío bloqueado; 200 chars inserta (sin límite longitud). Accumulate 12–20: `UNIQUE sku` excepción en harness (`run_12.log`). |
| B4 | Nombre acentos/ñ/emoji/comillas/500 | 20/20 | PASA | insertados=4 fallidos=0 (y contamina A1 vía ORDER BY nombre) |
| B5 | Cobrar pedido vacío | 20/20 | PASA | early return `PrincipalPanel.java:379-381` |
| B6 | Cobrar con caja CERRADA | **0/20** | **FALLA** | Venta con 0 cajas abiertas. Sin validación en `registrarVenta` (`PrincipalPanel.java:417+`). |
| B7 | Doble click Cobrar e imprimir | **0/20** | **FALLA** | Dos `registrarVenta` → 2 filas. Sin debounce en `procesarOrden`. |
| B8 | Abrir caja dos veces | 20/20 | PASA | guard `CajaDialog.java:341`; abiertas=1 |
| B9 | Cerrar caja con cobro a medio | **0/20** | **NO VERIFICADO** | Sin test de carrera hilos UI+DB. `registrarVenta` es sincrónico en EDT. |
| B10 | Merma > stock | 20/20 | PASA | confirma exceso `MermaDialog.java:188-195`; stock floorea en 0 |
| B11 | Compra cantidad 0 | 20/20 | PASA | `ComprasDialog.java:239-241` rechaza ≤0 |
| B12 | PIN incorrecto ×50 | **0/20** | **FALLA** | Sin bloqueo/backoff (`AdminPin.java:23-53` / `ComercioConfig.matchesAdminPin`). hitsFalsos=0 |

\*OK/N con RESULT explícito; en accumulate el test abortó por SQLException antes de `RESULT`.

---

## Fallos estables (producto)

1. **A10** — anchos mínimos ≤150; editor del combo sin `styleField`.
2. **A11** — leyenda donut con `break` por altura.
3. **B1** — bordes de cantidad rotos; especialmente `1.5 → 15`.
4. **B2** — precio 0 válido al guardar.
5. **B6** — cobro permitido con caja cerrada.
6. **B7** — doble cobro registra dos ventas.
7. **B12** — sin lockout de PIN.

## FLAKY / contaminación

| ID | Corridas FALLA | Causa verificada |
|----|----------------|------------------|
| A1 | 12–20 | Catálogo acumulado + `ORDER BY nombre` → 1.er producto @$10 |
| A6 | 12–20 (excepción) | SKU fijo `SKU-NEW-1` ya insertado |
| B3 | 12–20 (excepción) | SKUs de borde reutilizados |

## NO VERIFICADO

- **B9** — concurrencia cerrar-caja / cobro.
- **A7** (parcial) — click real AssertJ tras cerrar popup.
- **A9** (parcial) — texto de botones nativos del L&F Windows.
- **A3** (parcial) — camino “No” solo vía early-return en código, no diálogo UI automatizado.

## Regresiones / deuda post-correcciones

Respecto a lo que las 13 correcciones pretendían cerrar y **sigue roto o se rompe en borde**:

| Área | Qué se “arregló” | Qué demuestra la suite |
|------|------------------|------------------------|
| Cantidad spinner | `commitEdit` | A1 OK en DB limpia; B1 sigue mal (`1.5→15`, inválidos→1) |
| Layouts diálogos | preferred 160 | A10 FALLA: min 100–120; combo editor 128 |
| Donut 2 columnas | leyenda multi-tipo | A11 FALLA bajo altura realista 140px |
| PIN hasheado | hash en properties | B12: sin rate-limit / bloqueo |
| Caja / ventas turno | A8 PASA | B6: venta sin caja abierta |
| Cobrar | — | B7: sin protección doble click |
| Producto→stock | A6 PASA limpio | Accumulate + SKU fijo expone fragilidad; constraint DB sí existe |

No se modificó código de producto en esta fase (solo harness A7/A9 previo a las 20 corridas).
