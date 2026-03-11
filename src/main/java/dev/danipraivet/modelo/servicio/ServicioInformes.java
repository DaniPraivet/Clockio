package dev.danipraivet.modelo.servicio;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import dev.danipraivet.modelo.entidades.Empleado;
import dev.danipraivet.modelo.entidades.Fichaje;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * MODELO — Generación de informes de asistencia en PDF y Excel.
 *
 * El controlador abre el FileChooser, obtiene el File destino
 * y llama a estos métodos. La lógica de presentación del informe
 * vive aquí, no en el controlador.
 */
public class ServicioInformes {

    private static final Logger log = LoggerFactory.getLogger(ServicioInformes.class);

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA  = DateTimeFormatter.ofPattern("HH:mm");

    //Colores globales
    private static final DeviceRgb COLOR_CABECERA   = new DeviceRgb(21,  101, 192); // Azul
    private static final DeviceRgb COLOR_FILA_PAR   = new DeviceRgb(232, 240, 254);
    private static final DeviceRgb COLOR_FILA_IMPAR = new DeviceRgb(255, 255, 255);

    /**
     * Genera un PDF con el informe de asistencia del empleado en el rango de fechas.
     *
     * @param empleado Empleado del informe (null = todos los empleados)
     * @param fichajes Lista de fichajes a incluir
     * @param desde Fecha de inicio del rango
     * @param hasta Fecha de fin del rango
     * @param destino Fichero donde guardar el PDF
     * @return true si se generó correctamente
     */
    public boolean generarPDF(Empleado empleado, List<Fichaje> fichajes,
                              LocalDate desde, LocalDate hasta, File destino) {
        try {
            PdfWriter   writer   = new PdfWriter(destino);
            PdfDocument pdfDoc   = new PdfDocument(writer);
            Document    document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(36, 36, 36, 36);

            // Cabecera
            Table cabecera = new Table(UnitValue.createPercentArray(new float[]{1, 3}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Logo
            URL logoUrl = getClass().getResource("/dev/danipraivet/logo/logo.png");
            if (logoUrl != null) {
                Image logo = new Image(ImageDataFactory.create(logoUrl))
                        .setHeight(50).setAutoScale(false);
                cabecera.addCell(new Cell().add(logo)
                        .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            } else {
                cabecera.addCell(new Cell()
                        .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            }

            // Título
            Paragraph titulo = new Paragraph("INFORME DE ASISTENCIA")
                    .setFontSize(18).setBold()
                    .setFontColor(COLOR_CABECERA)
                    .setTextAlignment(TextAlignment.RIGHT);
            Paragraph subtitulo = new Paragraph(
                    "Período: " + desde.format(FMT_FECHA) + " — " + hasta.format(FMT_FECHA))
                    .setFontSize(10).setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.RIGHT);
            cabecera.addCell(new Cell()
                    .add(titulo).add(subtitulo)
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));

            document.add(cabecera);
            document.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f))
                    .setMarginTop(4).setMarginBottom(8));

            // Datos del empleado
            if (empleado != null) {
                Table infoEmp = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                        .setWidth(UnitValue.createPercentValue(100))
                        .setMarginBottom(12);

                infoEmp.addCell(celda("Empleado:", true));
                infoEmp.addCell(celda(empleado.getNombreCompleto(), false));
                infoEmp.addCell(celda("DNI:", true));
                infoEmp.addCell(celda(empleado.getDni(), false));
                infoEmp.addCell(celda("Departamento:", true));
                infoEmp.addCell(celda(empleado.getDepartamento() != null
                        ? empleado.getDepartamento().getNombre() : "—", false));

                document.add(infoEmp);
            }

            // Tabla de fichajes
            float[] anchos = {2, 1.5f, 1.5f, 1.5f, 1.5f, 2};
            Table tabla = new Table(UnitValue.createPercentArray(anchos))
                    .setWidth(UnitValue.createPercentValue(100));

            // Cabecera de tabla
            String[] columnas = {"Fecha", "Entrada", "Salida", "Horas", "Extras", "Estado"};
            for (String col : columnas) {
                tabla.addHeaderCell(new Cell()
                        .add(new Paragraph(col).setBold().setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(COLOR_CABECERA)
                        .setTextAlignment(TextAlignment.CENTER));
            }

            // Filas
            double totalHoras = 0, totalExtras = 0;
            for (int i = 0; i < fichajes.size(); i++) {
                Fichaje f = fichajes.get(i);
                DeviceRgb colorFila = (i % 2 == 0) ? COLOR_FILA_PAR : COLOR_FILA_IMPAR;

                tabla.addCell(fila(f.getFecha() != null ? f.getFecha().format(FMT_FECHA) : "—", colorFila, false));
                tabla.addCell(fila(f.getEntradaHora() != null ? f.getEntradaHora().format(FMT_HORA) : "--:--", colorFila, true));
                tabla.addCell(fila(f.getSalidaHora()  != null ? f.getSalidaHora().format(FMT_HORA)  : "--:--", colorFila, true));
                tabla.addCell(fila(f.getHorasFormateadas(), colorFila, true));

                BigDecimal extras = f.getHorasExtras();
                tabla.addCell(fila(extras != null && extras.compareTo(BigDecimal.ZERO) > 0
                        ? extras + "h" : "—", colorFila, true));
                tabla.addCell(fila(f.getEstado(), colorFila, true));

                if (f.getHorasTrabajadas() != null) totalHoras  += f.getHorasTrabajadas().doubleValue();
                if (f.getHorasExtras()     != null) totalExtras += f.getHorasExtras().doubleValue();
            }

            document.add(tabla);

            // Resumen
            Paragraph resumen = new Paragraph(String.format(
                    "Total días: %d   |   Total horas trabajadas: %.1f h   |   Horas extra: %.1f h",
                    fichajes.size(), totalHoras, totalExtras))
                    .setFontSize(10).setBold().setMarginTop(10)
                    .setTextAlignment(TextAlignment.RIGHT);
            document.add(resumen);

            // Pie de página
            Paragraph pie = new Paragraph("Clockio — Sistema de Control de Asistencia   |   " +
                    "Generado el " + LocalDate.now().format(FMT_FECHA))
                    .setFontSize(8).setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(24);
            document.add(pie);

            document.close();
            log.info("PDF generado correctamente: {}", destino.getAbsolutePath());
            return true;

        } catch (Exception e) {
            log.error("Error generando PDF: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Genera un Excel (.xlsx) con el informe de asistencia.
     */
    public boolean generarExcel(Empleado empleado, List<Fichaje> fichajes,
                                LocalDate desde, LocalDate hasta, File destino) {
        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Asistencia");
            sheet.setColumnWidth(0, 4000); // Fecha
            sheet.setColumnWidth(1, 3000); // Entrada
            sheet.setColumnWidth(2, 3000); // Salida
            sheet.setColumnWidth(3, 3000); // Horas
            sheet.setColumnWidth(4, 3000); // Extras
            sheet.setColumnWidth(5, 3500); // Estado

            // Estilos
            CellStyle estiloCabecera = workbook.createCellStyle();
            estiloCabecera.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            estiloCabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloCabecera.setAlignment(HorizontalAlignment.CENTER);
            estiloCabecera.setBorderBottom(BorderStyle.THIN);
            Font fuenteCabecera = workbook.createFont();
            fuenteCabecera.setBold(true);
            fuenteCabecera.setColor(IndexedColors.WHITE.getIndex());
            estiloCabecera.setFont(fuenteCabecera);

            CellStyle estiloPar = workbook.createCellStyle();
            estiloPar.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            estiloPar.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloPar.setAlignment(HorizontalAlignment.CENTER);

            CellStyle estiloImpar = workbook.createCellStyle();
            estiloImpar.setAlignment(HorizontalAlignment.CENTER);

            CellStyle estiloTitulo = workbook.createCellStyle();
            Font fuenteTitulo = workbook.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            fuenteTitulo.setColor(IndexedColors.ROYAL_BLUE.getIndex());
            estiloTitulo.setFont(fuenteTitulo);

            CellStyle estiloInfo = workbook.createCellStyle();
            Font fuenteInfo = workbook.createFont();
            fuenteInfo.setBold(true);
            estiloInfo.setFont(fuenteInfo);

            int fila = 0;

            // Titulo
            Row rowTitulo = sheet.createRow(fila++);
            org.apache.poi.ss.usermodel.Cell celdaTitulo = rowTitulo.createCell(0);
            celdaTitulo.setCellValue("INFORME DE ASISTENCIA — Clockio");
            celdaTitulo.setCellStyle(estiloTitulo);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            Row rowPeriodo = sheet.createRow(fila++);
            rowPeriodo.createCell(0).setCellValue(
                    "Período: " + desde.format(FMT_FECHA) + " — " + hasta.format(FMT_FECHA));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

            // Datos del empleado
            if (empleado != null) {
                fila++;
                Row rowEmp = sheet.createRow(fila++);
                org.apache.poi.ss.usermodel.Cell c = rowEmp.createCell(0);
                c.setCellValue("Empleado:");
                c.setCellStyle(estiloInfo);
                rowEmp.createCell(1).setCellValue(empleado.getNombreCompleto());

                Row rowDni = sheet.createRow(fila++);
                org.apache.poi.ss.usermodel.Cell cDni = rowDni.createCell(0);
                cDni.setCellValue("DNI:");
                cDni.setCellStyle(estiloInfo);
                rowDni.createCell(1).setCellValue(empleado.getDni());

                Row rowDep = sheet.createRow(fila++);
                org.apache.poi.ss.usermodel.Cell cDep = rowDep.createCell(0);
                cDep.setCellValue("Departamento:");
                cDep.setCellStyle(estiloInfo);
                rowDep.createCell(1).setCellValue(empleado.getDepartamento() != null
                        ? empleado.getDepartamento().getNombre() : "—");
            }

            fila++;

            // Cabecera de la tabla
            Row rowCabecera = sheet.createRow(fila++);
            String[] cols = {"Fecha", "Entrada", "Salida", "Horas", "Extras", "Estado"};
            for (int i = 0; i < cols.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = rowCabecera.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(estiloCabecera);
            }

            // Filas de datos
            double totalHoras = 0, totalExtras = 0;
            for (int i = 0; i < fichajes.size(); i++) {
                Fichaje f = fichajes.get(i);
                CellStyle estilo = (i % 2 == 0) ? estiloPar : estiloImpar;
                Row row = sheet.createRow(fila++);

                setValue(row, 0, f.getFecha() != null ? f.getFecha().format(FMT_FECHA) : "—", estilo);
                setValue(row, 1, f.getEntradaHora() != null ? f.getEntradaHora().format(FMT_HORA) : "--:--", estilo);
                setValue(row, 2, f.getSalidaHora()  != null ? f.getSalidaHora().format(FMT_HORA)  : "--:--", estilo);
                setValue(row, 3, f.getHorasFormateadas(), estilo);

                BigDecimal extras = f.getHorasExtras();
                setValue(row, 4, extras != null && extras.compareTo(BigDecimal.ZERO) > 0
                        ? extras + "h" : "—", estilo);
                setValue(row, 5, f.getEstado(), estilo);

                if (f.getHorasTrabajadas() != null) totalHoras  += f.getHorasTrabajadas().doubleValue();
                if (f.getHorasExtras()     != null) totalExtras += f.getHorasExtras().doubleValue();
            }

            // Resumen
            fila++;
            Row rowResumen = sheet.createRow(fila);
            org.apache.poi.ss.usermodel.Cell cResumen = rowResumen.createCell(0);
            cResumen.setCellValue(String.format(
                    "Total días: %d   |   Horas trabajadas: %.1f h   |   Horas extra: %.1f h",
                    fichajes.size(), totalHoras, totalExtras));
            cResumen.setCellStyle(estiloInfo);
            sheet.addMergedRegion(new CellRangeAddress(fila, fila, 0, 5));

            // Guardar
            try (FileOutputStream fos = new FileOutputStream(destino)) {
                workbook.write(fos);
            }

            log.info("Excel generado correctamente: {}", destino.getAbsolutePath());
            return true;

        } catch (Exception e) {
            log.error("Error generando Excel: {}", e.getMessage(), e);
            return false;
        }
    }

    private Cell celda(String texto, boolean negrita) {
        Paragraph p = new Paragraph(texto).setFontSize(10);
        if (negrita) p.setBold();
        return new Cell().add(p)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
    }

    private Cell fila(String texto, DeviceRgb color, boolean centrar) {
        Paragraph p = new Paragraph(texto).setFontSize(9);
        if (centrar) p.setTextAlignment(TextAlignment.CENTER);
        return new Cell().add(p).setBackgroundColor(color);
    }

    private void setValue(Row row, int col, String value, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
}