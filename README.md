# Sistema Pizzería Febros

Aplicación de caja local (sin internet) para pizzerías chicas.

## Qué incluye

- Armar pedido con productos, cantidad y nota/aderezo
- Precio editable por ítem
- Ticket de venta (impresión o vista previa)
- Ticket repartidor con dirección
- Alta/edición de productos y precios
- Informe del día (total vendido)
- Base SQLite portable junto al JAR

## Instalación en la PC del cliente

1. Copiar la carpeta `ProyectoMago/dist` completa:
   - `ProyectoMago.jar`
   - `lib/sqlite-jdbc-3.39.3.0.jar`
   - `prueba.db`
   - `Iniciar.bat`
   - `descarga.png` (logo opcional)
2. Tener Java 17+ instalado
3. Doble clic en `Iniciar.bat`

## Desarrollo

```bat
ant -f ProyectoMago\build.xml jar
```
