package com.example.examen2.reporte.service;

import com.example.examen2.asistencia.model.Asistencia;
import com.example.examen2.asistencia.model.Licencia;
import com.example.examen2.asistencia.repository.AsistenciaRepository;
import com.example.examen2.asistencia.repository.LicenciaRepository;
import com.example.examen2.reporte.model.Reporte;
import com.example.examen2.reporte.model.ReporteDTO;
import com.example.examen2.reporte.repository.ReporteRepository;
import com.example.examen2.reporte.repository.TipoReporteRepository;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    @Autowired
    private ReporteRepository reporteRepository;

    @Autowired
    private TipoReporteRepository tipoReporteRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private LicenciaRepository licenciaRepository;

    private static final String REPORTS_DIR = "src/main/resources/static/reports/";
    private static int pdfCounter = 0; // Contador para verificar llamadas

    public List<ReporteDTO> findAll() {
        return reporteRepository.findAll().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    public ReporteDTO findById(Long id) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte not found with id: " + id));
        return mapEntityToDto(reporte);
    }

    public ReporteDTO createReporte(ReporteDTO reporteDTO) {
        Reporte reporte = mapDtoToEntity(reporteDTO);
        return mapEntityToDto(reporteRepository.save(reporte));
    }

    public ReporteDTO updateReporte(Long id, ReporteDTO reporteDTO) {
        Reporte existingReporte = reporteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte not found with id: " + id));
        updateEntityFromDto(existingReporte, reporteDTO);
        return mapEntityToDto(reporteRepository.save(existingReporte));
    }

    public void deleteReporte(Long id) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte not found with id: " + id));
        reporteRepository.delete(reporte);
    }

    private Reporte mapDtoToEntity(ReporteDTO dto) {
        Reporte reporte = new Reporte();
        reporte.setFechaGeneracion(dto.getFechaGeneracion());
        reporte.setPeriodo(dto.getPeriodo());
        reporte.setTipoReporte(tipoReporteRepository.findById(dto.getTipoReporteId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid tipoReporteId: " + dto.getTipoReporteId())));
        return reporte;
    }

    private void updateEntityFromDto(Reporte reporte, ReporteDTO dto) {
        reporte.setPeriodo(dto.getPeriodo());
        reporte.setTipoReporte(tipoReporteRepository.findById(dto.getTipoReporteId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid tipoReporteId: " + dto.getTipoReporteId())));
    }

    private ReporteDTO mapEntityToDto(Reporte reporte) {
        ReporteDTO dto = new ReporteDTO();
        dto.setId(reporte.getId());
        dto.setFechaGeneracion(reporte.getFechaGeneracion());
        dto.setPeriodo(reporte.getPeriodo());
        dto.setTipoReporteId(reporte.getTipoReporte().getId());
        dto.setPdfPath(reporte.getPdfPath());
        dto.setExcelPath(reporte.getExcelPath());
        dto.setAsistenciaIds(reporte.getAsistencias() != null ?
                reporte.getAsistencias().stream().map(Asistencia::getId).collect(Collectors.toList()) : new ArrayList<>());
        return dto;
    }

    public ByteArrayInputStream generateReporteAsistenciasPdf(List<Asistencia> asistencias, Long reporteId) throws IOException {
        pdfCounter++; // Incrementa el contador
        System.out.println("generateReporteAsistenciasPdf called " + pdfCounter + " times");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        Document document = new Document(new com.itextpdf.kernel.pdf.PdfDocument(writer));

        document.add(new Paragraph("Reporte de Asistencias"));

        Table table = new Table(7);
        table.addHeaderCell(new Cell().add(new Paragraph("ID")));
        table.addHeaderCell(new Cell().add(new Paragraph("Docente")));
        table.addHeaderCell(new Cell().add(new Paragraph("Fecha")));
        table.addHeaderCell(new Cell().add(new Paragraph("Estado")));
        table.addHeaderCell(new Cell().add(new Paragraph("Observaciones")));
        table.addHeaderCell(new Cell().add(new Paragraph("Materia")));
        table.addHeaderCell(new Cell().add(new Paragraph("Grupo")));

        for (Asistencia asistencia : asistencias) {
            table.addCell(new Cell().add(new Paragraph(asistencia.getId().toString())));
            table.addCell(new Cell().add(new Paragraph(asistencia.getDocente().getNombre())));
            table.addCell(new Cell().add(new Paragraph(asistencia.getFecha().toString())));
            table.addCell(new Cell().add(new Paragraph(asistencia.getEstadoAsistencia().getNombre())));
            table.addCell(new Cell().add(new Paragraph(asistencia.getObservaciones())));
            table.addCell(new Cell().add(new Paragraph(asistencia.getProgramacionAcademica().getMateria().getNombre())));
            table.addCell(new Cell().add(new Paragraph(asistencia.getProgramacionAcademica().getGrupo())));
        }

        document.add(table);
        document.close();

        // Guardar el archivo en el sistema de archivos
        Path path = Paths.get(REPORTS_DIR, "reporte_" + reporteId + ".pdf");
        Files.createDirectories(path.getParent());
        Files.write(path, out.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // Actualizar el path del PDF en el reporte
        Reporte reporte = reporteRepository.findById(reporteId).orElseThrow(() -> new IllegalArgumentException("Reporte not found with id: " + reporteId));
        reporte.setPdfPath("/static/reports/reporte_" + reporteId + ".pdf");
        reporteRepository.save(reporte);

        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream generateReporteAsistenciasExcel(List<Asistencia> asistencias, Long reporteId) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Sheet sheet = workbook.createSheet("Asistencias");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("Docente");
        headerRow.createCell(2).setCellValue("Fecha");
        headerRow.createCell(3).setCellValue("Estado");
        headerRow.createCell(4).setCellValue("Observaciones");
        headerRow.createCell(5).setCellValue("Materia");
        headerRow.createCell(6).setCellValue("Grupo");

        int rowIdx = 1;
        for (Asistencia asistencia : asistencias) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(asistencia.getId());
            row.createCell(1).setCellValue(asistencia.getDocente().getNombre());
            row.createCell(2).setCellValue(asistencia.getFecha().toString());
            row.createCell(3).setCellValue(asistencia.getEstadoAsistencia().getNombre());
            row.createCell(4).setCellValue(asistencia.getObservaciones());
            row.createCell(5).setCellValue(asistencia.getProgramacionAcademica().getMateria().getNombre());
            row.createCell(6).setCellValue(asistencia.getProgramacionAcademica().getGrupo());
        }

        workbook.write(out);
        workbook.close();

        // Guardar el archivo en el sistema de archivos
        Path path = Paths.get(REPORTS_DIR, "reporte_" + reporteId + ".xlsx");
        Files.createDirectories(path.getParent());
        Files.write(path, out.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // Actualizar el path del Excel en el reporte
        Reporte reporte = reporteRepository.findById(reporteId).orElseThrow(() -> new IllegalArgumentException("Reporte not found with id: " + reporteId));
        reporte.setExcelPath("/static/reports/reporte_" + reporteId + ".xlsx");
        reporteRepository.save(reporte);

        return new ByteArrayInputStream(out.toByteArray());
    }

    public ReporteDTO createReporteFromAsistencias(List<Asistencia> asistencias, Long tipoReporteId) {
        Reporte reporte = new Reporte();
        reporte.setFechaGeneracion(new Date());
        reporte.setPeriodo("Periodo de Reporte");
        reporte.setTipoReporte(tipoReporteRepository.findById(tipoReporteId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid tipoReporteId: " + tipoReporteId)));
        reporte.setAsistencias(asistencias);
        Reporte savedReporte = reporteRepository.save(reporte);

        return mapEntityToDto(savedReporte);
    }

    public ReporteDTO createReportePdfFromAsistencias(List<Asistencia> asistencias, Long tipoReporteId) throws IOException {
        ReporteDTO reporteDTO = createReporteFromAsistencias(asistencias, tipoReporteId);
        generateReporteAsistenciasPdf(asistencias, reporteDTO.getId());
        return reporteDTO;
    }

    public ReporteDTO createReporteExcelFromAsistencias(List<Asistencia> asistencias, Long tipoReporteId) throws IOException {
        ReporteDTO reporteDTO = createReporteFromAsistencias(asistencias, tipoReporteId);
        generateReporteAsistenciasExcel(asistencias, reporteDTO.getId());
        return reporteDTO;
    }

    public List<Asistencia> findAsistenciasByFilters(Date fechaInicio, Date fechaFin, String materia, String docente) {
        return asistenciaRepository.findAll().stream()
                .filter(asistencia -> asistencia.getEstadoAsistencia().getNombre().equalsIgnoreCase("PRESENTE"))
                .filter(asistencia -> {
                    boolean matches = true;
                    if (fechaInicio != null && fechaFin != null) {
                        matches = !asistencia.getFecha().before(fechaInicio) && !asistencia.getFecha().after(fechaFin);
                    } else if (fechaInicio != null) {
                        matches = !asistencia.getFecha().before(fechaInicio);
                    } else if (fechaFin != null) {
                        matches = !asistencia.getFecha().after(fechaFin);
                    }
                    if (materia != null && !materia.isEmpty()) {
                        matches = matches && asistencia.getProgramacionAcademica().getMateria().getNombre().equalsIgnoreCase(materia);
                    }
                    if (docente != null && !docente.isEmpty()) {
                        matches = matches && asistencia.getDocente().getNombre().equalsIgnoreCase(docente);
                    }
                    return matches;
                })
                .collect(Collectors.toList());
    }

    public List<Asistencia> findAtrasosByFilters(Date fechaInicio, Date fechaFin, String materia, String docente) {
        return asistenciaRepository.findAll().stream()
                .filter(asistencia -> asistencia.getEstadoAsistencia().getNombre().equalsIgnoreCase("RETRASO"))
                .filter(asistencia -> {
                    boolean matches = true;
                    if (fechaInicio != null && fechaFin != null) {
                        matches = !asistencia.getFecha().before(fechaInicio) && !asistencia.getFecha().after(fechaFin);
                    } else if (fechaInicio != null) {
                        matches = !asistencia.getFecha().before(fechaInicio);
                    } else if (fechaFin != null) {
                        matches = !asistencia.getFecha().after(fechaFin);
                    }
                    if (materia != null && !materia.isEmpty()) {
                        matches = matches && asistencia.getProgramacionAcademica().getMateria().getNombre().equalsIgnoreCase(materia);
                    }
                    if (docente != null && !docente.isEmpty()) {
                        matches = matches && asistencia.getDocente().getNombre().equalsIgnoreCase(docente);
                    }
                    return matches;
                })
                .collect(Collectors.toList());
    }

    public List<Asistencia> findFaltasByFilters(Date fechaInicio, Date fechaFin, String materia, String docente) {
        return asistenciaRepository.findAll().stream()
                .filter(asistencia -> asistencia.getEstadoAsistencia().getNombre().equalsIgnoreCase("AUSENTE"))
                .filter(asistencia -> {
                    boolean matches = true;
                    if (fechaInicio != null && fechaFin != null) {
                        matches = !asistencia.getFecha().before(fechaInicio) && !asistencia.getFecha().after(fechaFin);
                    } else if (fechaInicio != null) {
                        matches = !asistencia.getFecha().before(fechaInicio);
                    } else if (fechaFin != null) {
                        matches = !asistencia.getFecha().after(fechaFin);
                    }
                    if (materia != null && !materia.isEmpty()) {
                        matches = matches && asistencia.getProgramacionAcademica().getMateria().getNombre().equalsIgnoreCase(materia);
                    }
                    if (docente != null && !docente.isEmpty()) {
                        matches = matches && asistencia.getDocente().getNombre().equalsIgnoreCase(docente);
                    }
                    return matches;
                })
                .collect(Collectors.toList());
    }

    public List<Licencia> findLicenciasByFilters(Date fechaInicio, Date fechaFin, String materia, String docente) {
        return licenciaRepository.findAll().stream()
                .filter(licencia -> {
                    boolean matches = true;
                    if (fechaInicio != null && fechaFin != null) {
                        matches = !licencia.getFecha().before(fechaInicio) && !licencia.getFecha().after(fechaFin);
                    } else if (fechaInicio != null) {
                        matches = !licencia.getFecha().before(fechaInicio);
                    } else if (fechaFin != null) {
                        matches = !licencia.getFecha().after(fechaFin);
                    }
                    if (materia != null && !materia.isEmpty()) {
                        matches = matches && licencia.getProgramacionAcademica().getMateria().getNombre().equalsIgnoreCase(materia);
                    }
                    if (docente != null && !docente.isEmpty()) {
                        matches = matches && licencia.getDocente().getNombre().equalsIgnoreCase(docente);
                    }
                    return matches;
                })
                .collect(Collectors.toList());
    }

    // Métodos para generar PDFs y Excels de licencias...
    public ByteArrayInputStream generateReporteLicenciasPdf(List<Licencia> licencias, Long reporteId) throws IOException {
        pdfCounter++; // Incrementa el contador
        System.out.println("generateReporteLicenciasPdf called " + pdfCounter + " times");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        Document document = new Document(new com.itextpdf.kernel.pdf.PdfDocument(writer));

        document.add(new Paragraph("Reporte de Licencias"));

        Table table = new Table(5);
        table.addHeaderCell(new Cell().add(new Paragraph("ID")));
        table.addHeaderCell(new Cell().add(new Paragraph("Docente")));
        table.addHeaderCell(new Cell().add(new Paragraph("Fecha")));
        table.addHeaderCell(new Cell().add(new Paragraph("Motivo")));
        table.addHeaderCell(new Cell().add(new Paragraph("Materia")));

        for (Licencia licencia : licencias) {
            table.addCell(new Cell().add(new Paragraph(licencia.getId().toString())));
            table.addCell(new Cell().add(new Paragraph(licencia.getDocente().getNombre())));
            table.addCell(new Cell().add(new Paragraph(licencia.getFecha().toString())));
            table.addCell(new Cell().add(new Paragraph(licencia.getMotivo())));
            table.addCell(new Cell().add(new Paragraph(licencia.getProgramacionAcademica().getMateria().getNombre())));
        }

        document.add(table);
        document.close();

        // Guardar el archivo en el sistema de archivos
        Path path = Paths.get(REPORTS_DIR, "reporte_" + reporteId + ".pdf");
        Files.createDirectories(path.getParent());
        Files.write(path, out.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // Actualizar el path del PDF en el reporte
        Reporte reporte = reporteRepository.findById(reporteId).orElseThrow(() -> new IllegalArgumentException("Reporte not found with id: " + reporteId));
        reporte.setPdfPath("/static/reports/reporte_" + reporteId + ".pdf");
        reporteRepository.save(reporte);

        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream generateReporteLicenciasExcel(List<Licencia> licencias, Long reporteId) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Sheet sheet = workbook.createSheet("Licencias");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("Docente");
        headerRow.createCell(2).setCellValue("Fecha");
        headerRow.createCell(3).setCellValue("Motivo");
        headerRow.createCell(4).setCellValue("Materia");

        int rowIdx = 1;
        for (Licencia licencia : licencias) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(licencia.getId());
            row.createCell(1).setCellValue(licencia.getDocente().getNombre());
            row.createCell(2).setCellValue(licencia.getFecha().toString());
            row.createCell(3).setCellValue(licencia.getMotivo());
            row.createCell(4).setCellValue(licencia.getProgramacionAcademica().getMateria().getNombre());
        }

        workbook.write(out);
        workbook.close();

        // Guardar el archivo en el sistema de archivos
        Path path = Paths.get(REPORTS_DIR, "reporte_" + reporteId + ".xlsx");
        Files.createDirectories(path.getParent());
        Files.write(path, out.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // Actualizar el path del Excel en el reporte
        Reporte reporte = reporteRepository.findById(reporteId).orElseThrow(() -> new IllegalArgumentException("Reporte not found with id: " + reporteId));
        reporte.setExcelPath("/static/reports/reporte_" + reporteId + ".xlsx");
        reporteRepository.save(reporte);

        return new ByteArrayInputStream(out.toByteArray());
    }

    public ReporteDTO createReportePdfFromLicencias(List<Licencia> licencias, Long tipoReporteId) throws IOException {
        ReporteDTO reporteDTO = createReporteFromLicencias(licencias, tipoReporteId);
        generateReporteLicenciasPdf(licencias, reporteDTO.getId());
        return reporteDTO;
    }

    public ReporteDTO createReporteExcelFromLicencias(List<Licencia> licencias, Long tipoReporteId) throws IOException {
        ReporteDTO reporteDTO = createReporteFromLicencias(licencias, tipoReporteId);
        generateReporteLicenciasExcel(licencias, reporteDTO.getId());
        return reporteDTO;
    }

    private ReporteDTO createReporteFromLicencias(List<Licencia> licencias, Long tipoReporteId) {
        Reporte reporte = new Reporte();
        reporte.setFechaGeneracion(new Date());
        reporte.setPeriodo("Periodo de Reporte");
        reporte.setTipoReporte(tipoReporteRepository.findById(tipoReporteId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid tipoReporteId: " + tipoReporteId)));
        reporte.setLicencias(licencias);
        Reporte savedReporte = reporteRepository.save(reporte);

        return mapEntityToDto(savedReporte);
    }
}
