package proyectomago.panelclass;

import connectDatabase.conection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Lógica compartida de caja: estado, ventas del turno y esperado en efectivo.
 * Las ventas se miden desde {@code venta_detalle} a partir de la apertura
 * (no el total diario de {@code informes}, que falla con varias cajas o turno nocturno).
 */
public final class CajaService {

    private CajaService() {
    }

    public static final class CajaAbierta {
        public final int id;
        public final String fechaApertura;
        public final double montoApertura;

        public CajaAbierta(int id, String fechaApertura, double montoApertura) {
            this.id = id;
            this.fechaApertura = fechaApertura;
            this.montoApertura = montoApertura;
        }
    }

    public static CajaAbierta getAbierta() {
        String sql = "SELECT id, fecha_apertura, monto_apertura FROM cajas "
                + "WHERE estado = 'ABIERTA' ORDER BY id DESC LIMIT 1";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            return new CajaAbierta(
                    rs.getInt("id"),
                    rs.getString("fecha_apertura"),
                    rs.getDouble("monto_apertura"));
        } catch (SQLException e) {
            System.out.println("Error al leer caja abierta: " + e.getMessage());
            return null;
        }
    }

    public static boolean hayCajaAbierta() {
        return getAbierta() != null;
    }

    public static double sumMovimientos(int cajaId, String tipo) {
        String sql = "SELECT COALESCE(SUM(monto), 0) FROM movimientos_caja WHERE caja_id = ? AND tipo = ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, cajaId);
            ps.setString(2, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Suma de subtotales vendidos desde la apertura de esta caja (inclusive).
     */
    public static double sumVentasDesde(String fechaApertura) {
        if (fechaApertura == null || fechaApertura.isBlank()) {
            return 0;
        }
        String sql = "SELECT COALESCE(SUM(subtotal), 0) FROM venta_detalle WHERE fecha >= ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, fechaApertura);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    public static double esperadoEnCaja(CajaAbierta caja) {
        if (caja == null) {
            return 0;
        }
        double ventas = sumVentasDesde(caja.fechaApertura);
        double entradas = sumMovimientos(caja.id, "ENTRADA");
        double salidas = sumMovimientos(caja.id, "SALIDA");
        return round2(caja.montoApertura + ventas + entradas - salidas);
    }

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public static boolean montosCierran(double contado, double esperado) {
        return Math.abs(round2(contado) - round2(esperado)) < 0.009;
    }
}
