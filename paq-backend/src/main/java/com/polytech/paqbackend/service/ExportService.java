package com.polytech.paqbackend.service;

import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.polytech.paqbackend.dto.*;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {

    private final DashboardService dashboardService;

    public ExportService(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    public byte[] generatePdfReport(Long siteId, Long plantId) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        String title = "Rapport PAQ - " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        if (plantId != null) title += " - Plant: " + plantId;
        else if (siteId != null) title += " - Site: " + siteId;

        document.add(new Paragraph(title).setFontSize(20).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph(" "));

        // Statistiques générales
        DashboardStatsDTO stats = dashboardService.getStats(siteId, plantId);
        EntretiensTotalsDTO entretiensTotals = dashboardService.getEntretiensTotals(siteId, plantId);

        document.add(new Paragraph("1. Statistiques Générales").setFontSize(16).setBold());
        Table generalTable = new Table(UnitValue.createPercentArray(new float[]{2, 1})).setWidth(UnitValue.createPercentValue(100));
        generalTable.addCell("Total Collaborateurs");
        generalTable.addCell(String.valueOf(stats.getTotalCollaborateurs()));
        generalTable.addCell("PAQ en Cours");
        generalTable.addCell(String.valueOf(stats.getPaqEnCours()));
        generalTable.addCell("Sans Faute");
        generalTable.addCell(String.valueOf(stats.getSansFaute().size()));
        generalTable.addCell("Total Entretiens");
        generalTable.addCell(String.valueOf(entretiensTotals.getTotal()));
        document.add(generalTable);
        document.add(new Paragraph(" "));

        // Répartition des entretiens
        document.add(new Paragraph("2. Répartition des Entretiens").setFontSize(16).setBold());
        Table entretienTable = new Table(UnitValue.createPercentArray(new float[]{2, 1})).setWidth(UnitValue.createPercentValue(100));
        entretienTable.addCell("Entretien Explicatif");
        entretienTable.addCell(String.valueOf(entretiensTotals.getExplicatif()));
        entretienTable.addCell("Entretien d'Accord");
        entretienTable.addCell(String.valueOf(entretiensTotals.getAccord()));
        entretienTable.addCell("Entretien de Mesure");
        entretienTable.addCell(String.valueOf(entretiensTotals.getMesure()));
        entretienTable.addCell("Entretien de Décision");
        entretienTable.addCell(String.valueOf(entretiensTotals.getDecision()));
        entretienTable.addCell("Entretien Final");
        entretienTable.addCell(String.valueOf(entretiensTotals.getFinal()));
        document.add(entretienTable);
        document.add(new Paragraph(" "));

        // Évolution des entretiens
        document.add(new Paragraph("3. Évolution des Entretiens").setFontSize(16).setBold());
        List<EntretienEvolutionDTO> evolution = dashboardService.getEntretiensEvolution(siteId, plantId);
        Table evolutionTable = new Table(UnitValue.createPercentArray(new float[]{2, 1})).setWidth(UnitValue.createPercentValue(100));
        evolutionTable.addHeaderCell("Période");
        evolutionTable.addHeaderCell("Nombre");
        for (EntretienEvolutionDTO evo : evolution) {
            evolutionTable.addCell(evo.getPeriode());
            evolutionTable.addCell(String.valueOf(evo.getCount()));
        }
        document.add(evolutionTable);

        document.close();
        return outputStream.toByteArray();
    }

    public byte[] generateExcelReport(Long siteId, Long plantId) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Feuille 1: Statistiques générales
        Sheet generalSheet = workbook.createSheet("Statistiques Générales");
        DashboardStatsDTO stats = dashboardService.getStats(siteId, plantId);
        EntretiensTotalsDTO entretiensTotals = dashboardService.getEntretiensTotals(siteId, plantId);

        int rowNum = 0;
        Row headerRow = generalSheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Métrique");
        headerRow.createCell(1).setCellValue("Valeur");
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);

        createRow(generalSheet, rowNum++, "Total Collaborateurs", stats.getTotalCollaborateurs());
        createRow(generalSheet, rowNum++, "PAQ en Cours", stats.getPaqEnCours());
        createRow(generalSheet, rowNum++, "Sans Faute", stats.getSansFaute().size());
        createRow(generalSheet, rowNum++, "Total Entretiens", entretiensTotals.getTotal());

        generalSheet.autoSizeColumn(0);
        generalSheet.autoSizeColumn(1);

        // Feuille 2: Répartition entretiens
        Sheet entretienSheet = workbook.createSheet("Répartition Entretiens");
        rowNum = 0;
        headerRow = entretienSheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Type d'entretien");
        headerRow.createCell(1).setCellValue("Nombre");
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);

        createRow(entretienSheet, rowNum++, "Explicatif", entretiensTotals.getExplicatif());
        createRow(entretienSheet, rowNum++, "Accord", entretiensTotals.getAccord());
        createRow(entretienSheet, rowNum++, "Mesure", entretiensTotals.getMesure());
        createRow(entretienSheet, rowNum++, "Décision", entretiensTotals.getDecision());
        createRow(entretienSheet, rowNum++, "Final", entretiensTotals.getFinal());

        entretienSheet.autoSizeColumn(0);
        entretienSheet.autoSizeColumn(1);

        // Feuille 3: Évolution entretiens
        Sheet evolutionSheet = workbook.createSheet("Évolution Entretiens");
        rowNum = 0;
        headerRow = evolutionSheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Période");
        headerRow.createCell(1).setCellValue("Nombre");
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);

        List<EntretienEvolutionDTO> evolution = dashboardService.getEntretiensEvolution(siteId, plantId);
        for (EntretienEvolutionDTO evo : evolution) {
            Row row = evolutionSheet.createRow(rowNum++);
            row.createCell(0).setCellValue(evo.getPeriode());
            row.createCell(1).setCellValue(evo.getCount());
        }
        evolutionSheet.autoSizeColumn(0);
        evolutionSheet.autoSizeColumn(1);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    private void createRow(Sheet sheet, int rowNum, String label, long value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }
}