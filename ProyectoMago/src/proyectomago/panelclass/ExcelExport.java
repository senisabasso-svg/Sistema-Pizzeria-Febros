package proyectomago.panelclass;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Exporta una hoja simple en formato SpreadsheetML (.xls) que Excel abre sin librerías extra.
 */
public final class ExcelExport {

    private ExcelExport() {
    }

    public static void writeSheet(
            File file,
            String sheetName,
            List<String[]> rows
    ) throws Exception {
        try (BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
            out.write("<?mso-application progid=\"Excel.Sheet\"?>\r\n");
            out.write("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\r\n");
            out.write(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\r\n");
            out.write(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\r\n");
            out.write(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\r\n");
            out.write("<Styles>\r\n");
            out.write("<Style ss:ID=\"Default\" ss:Name=\"Normal\"><Font ss:FontName=\"Calibri\" ss:Size=\"11\"/></Style>\r\n");
            out.write("<Style ss:ID=\"Title\"><Font ss:FontName=\"Calibri\" ss:Size=\"14\" ss:Bold=\"1\"/></Style>\r\n");
            out.write("<Style ss:ID=\"Header\"><Font ss:Bold=\"1\"/><Interior ss:Color=\"#FFE6CC\" ss:Pattern=\"Solid\"/></Style>\r\n");
            out.write("<Style ss:ID=\"Muted\"><Font ss:Color=\"#666666\"/></Style>\r\n");
            out.write("</Styles>\r\n");
            out.write("<Worksheet ss:Name=\"");
            out.write(xml(sheetName == null || sheetName.isBlank() ? "Informe" : sheetName));
            out.write("\">\r\n<Table>\r\n");

            for (String[] row : rows) {
                if (row == null) {
                    continue;
                }
                String style = row.length > 0 && row[0] != null && row[0].startsWith("__STYLE__")
                        ? row[0].substring(9)
                        : null;
                out.write("<Row>\r\n");
                int start = style != null ? 1 : 0;
                for (int i = start; i < row.length; i++) {
                    String value = row[i] == null ? "" : row[i];
                    boolean number = isPlainNumber(value);
                    out.write("<Cell");
                    if (style != null) {
                        out.write(" ss:StyleID=\"");
                        out.write(xml(style));
                        out.write("\"");
                    }
                    out.write("><Data ss:Type=\"");
                    out.write(number ? "Number" : "String");
                    out.write("\">");
                    out.write(xml(number ? value : value));
                    out.write("</Data></Cell>\r\n");
                }
                out.write("</Row>\r\n");
            }

            out.write("</Table>\r\n</Worksheet>\r\n</Workbook>\r\n");
        }
    }

    private static boolean isPlainNumber(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.matches("-?\\d+(\\.\\d+)?");
    }

    private static String xml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
