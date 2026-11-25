# Graph Report - ProyectoMago  (2026-11-21)

## Corpus Check
- 29 files · ~27,436 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 38 file(s) not represented in the graph (top: .log 21, .properties 5, .xml 4)
- WARNING: 38 of 72 files seen (53%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 589 nodes · 1827 edges · 21 communities (9 shown, 12 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 97 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- PrincipalPanel
- javax.swing.JButton
- AlmacenQaSuite
- conection
- UiTheme.java
- ComercioConfig
- Pedido
- ComprasDialog
- ProductosDialog
- StockDialog
- _seed_panaderia.py
- MenuPrecios
- ProyectoMago.java
- ExcelExport
- Catalogos

## God Nodes (most connected - your core abstractions)
1. `conection` - 64 edges
2. `PrincipalPanel` - 57 edges
3. `InformesDialog` - 37 edges
4. `AlmacenQaSuite` - 35 edges
5. `CajaDialog` - 31 edges
6. `ProductosDialog` - 28 edges
7. `ComprasDialog` - 27 edges
8. `StockDialog` - 26 edges
9. `ComercioConfig` - 23 edges
10. `ConfiguracionDialog` - 23 edges

## Surprising Connections (you probably didn't know these)
- `Reporte QA — ProyectoMago (Gestión almacenes)` --references--> `AlmacenQaSuite`  [INFERRED]
  qa-reports/REPORTE_QA.md → qa-tests/src/AlmacenQaSuite.java
- `MermaDialog` --references--> `KeyValue`  [EXTRACTED]
  src/proyectomago/panelclass/MermaDialog.java → src/proyectomago/panelclass/KeyValue.java
- `PrincipalPanel` --references--> `Pedido`  [EXTRACTED]
  src/proyectomago/panelclass/PrincipalPanel.java → src/proyectomago/panelclass/Pedido.java
- `ConfiguracionDialog` --references--> `ComercioConfig`  [EXTRACTED]
  src/proyectomago/panelclass/ConfiguracionDialog.java → src/proyectomago/panelclass/ComercioConfig.java
- `ComprasDialog` --references--> `KeyValue`  [EXTRACTED]
  src/proyectomago/panelclass/ComprasDialog.java → src/proyectomago/panelclass/KeyValue.java

## Import Cycles
- None detected.

## Communities (21 total, 12 thin omitted)

### Community 0 - "PrincipalPanel"
Cohesion: 0.05
Nodes (8): AdminPin, DonutChartPanel, Slice, InformesDialog, LoginPanel, JPanel, PrincipalPanel, RoundedPanel

### Community 2 - "javax.swing.JButton"
Cohesion: 0.08
Nodes (5): MermaDialog, DefaultTableModel, ProveedoresDialog, DefaultTableModel, UiTheme

### Community 4 - "conection"
Cohesion: 0.12
Nodes (5): conection, CajaDialog, DefaultTableModel, DefaultTableModel, VendedoresDialog

### Community 5 - "UiTheme.java"
Cohesion: 0.06
Nodes (5): DefaultTableModel, DefaultTableCellRenderer, MouseAdapter, BasicComboBoxUI, DefaultListCellRenderer

### Community 6 - "ComercioConfig"
Cohesion: 0.09
Nodes (8): Fallos estables (producto), FLAKY / contaminación, NO VERIFICADO, Regresiones / deuda post-correcciones, Reporte QA — ProyectoMago (Gestión almacenes), Tabla resumen, ComercioConfig, ConfiguracionDialog

### Community 7 - "Pedido"
Cohesion: 0.08
Nodes (3): Pedido, PrintResult, TicketPrinter

### Community 8 - "ComprasDialog"
Cohesion: 0.13
Nodes (3): ComprasDialog, DefaultTableModel, KeyValue

### Community 10 - "ProductosDialog"
Cohesion: 0.20
Nodes (3): ProductosDialog, DefaultTableModel, javax.swing.event.DocumentListener

### Community 12 - "_seed_panaderia.py"
Cohesion: 0.17
Nodes (3): parse(), build_products(), main()

## Knowledge Gaps
- **12 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PrincipalPanel` connect `PrincipalPanel` to `InformesDialog.java`, `javax.swing.JButton`, `AlmacenQaSuite`, `conection`, `UiTheme.java`, `Pedido`, `AlmacenQaSuite.java`?**
  _High betweenness centrality (0.112) - this node is a cross-community bridge._
- **Should `PrincipalPanel` be split into smaller, more focused modules?**
  _Cohesion score 0.051685393258426963 - nodes in this community are weakly interconnected._
- **Why does `conection` connect `conection` to `PrincipalPanel`, `InformesDialog.java`, `javax.swing.JButton`, `AlmacenQaSuite`, `ComprasDialog`, `ProductosDialog`, `StockDialog`?**
  _High betweenness centrality (0.074) - this node is a cross-community bridge._
- **Should `InformesDialog.java` be split into smaller, more focused modules?**
  _Cohesion score 0.11026156941649899 - nodes in this community are weakly interconnected._
- **Why does `ComercioConfig` connect `ComercioConfig` to `PrincipalPanel`, `AlmacenQaSuite.java`, `ComercioConfig.java`?**
  _High betweenness centrality (0.059) - this node is a cross-community bridge._
- **Should `javax.swing.JButton` be split into smaller, more focused modules?**
  _Cohesion score 0.08055152394775036 - nodes in this community are weakly interconnected._
- **Should `conection` be split into smaller, more focused modules?**
  _Cohesion score 0.11702127659574468 - nodes in this community are weakly interconnected._