package com.polytech.paqbackend.controller;

import com.polytech.paqbackend.dto.DashboardStatsDTO;
import com.polytech.paqbackend.dto.EntretienEvolutionDTO;
import com.polytech.paqbackend.dto.EntretiensTotalsDTO;
import com.polytech.paqbackend.service.DashboardService;
import com.polytech.paqbackend.service.ExportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@SuppressWarnings({"unused", "WeakerAccess"})
public class DashboardController {

    private final DashboardService dashboardService;
    private final ExportService exportService;

    public DashboardController(DashboardService dashboardService, ExportService exportService) {
        this.dashboardService = dashboardService;
        this.exportService = exportService;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getStats(
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long plantId) {
        return ResponseEntity.ok(dashboardService.getStats(siteId, plantId));
    }

    @GetMapping("/entretiens/totals")
    public ResponseEntity<EntretiensTotalsDTO> getEntretiensTotals(
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long plantId) {
        return ResponseEntity.ok(dashboardService.getEntretiensTotals(siteId, plantId));
    }

    @GetMapping("/entretiens/evolution")
    public ResponseEntity<List<EntretienEvolutionDTO>> getEntretiensEvolution(
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long plantId) {
        return ResponseEntity.ok(dashboardService.getEntretiensEvolution(siteId, plantId));
    }

    @GetMapping("/export/{format}")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable String format,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long plantId) {
        try {
            byte[] reportData;
            String filename;
            String contentType;

            if ("pdf".equalsIgnoreCase(format)) {
                reportData = exportService.generatePdfReport(siteId, plantId);
                filename = "rapport-semestriel.pdf";
                contentType = MediaType.APPLICATION_PDF_VALUE;
            } else if ("excel".equalsIgnoreCase(format)) {
                reportData = exportService.generateExcelReport(siteId, plantId);
                filename = "rapport-semestriel.xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            } else {
                return ResponseEntity.badRequest().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(reportData.length);

            return ResponseEntity.ok().headers(headers).body(reportData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}