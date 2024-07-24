package com.example.examen2.asistencia.service;

import com.example.examen2.asistencia.model.Asistencia;
import com.example.examen2.asistencia.model.AsistenciaDTO;
import com.example.examen2.asistencia.repository.AsistenciaRepository;
import com.example.examen2.asistencia.repository.EstadoAsistenciaRepository;
import com.example.examen2.docente.model.Docente;
import com.example.examen2.docente.repository.DocenteRepository;
import com.example.examen2.programacionacademica.model.ProgramacionAcademica;
import com.example.examen2.programacionacademica.model.SesionClase;
import com.example.examen2.programacionacademica.repository.ProgramacionAcademicaRepository;
import com.example.examen2.modulo.model.Aula;
import com.example.examen2.modulo.repository.AulaRepository;
import com.example.examen2.config.model.GeoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class AsistenciaService {

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private ProgramacionAcademicaRepository programacionAcademicaRepository;

    @Autowired
    private EstadoAsistenciaRepository estadoAsistenciaRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private AulaRepository aulaRepository;

    private static final double MAX_DISTANCE = 0.5; // Distancia máxima en kilómetros
    private static final long TOLERANCE_MINUTES = 10; // Tolerancia en minutos para considerar presente
    private static final long ESTADO_PRESENTE = 1;
    private static final long ESTADO_RETRASO = 2;
    private static final long ESTADO_AUSENTE = 3;

    public List<AsistenciaDTO> findAll() {
        return asistenciaRepository.findAll().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    public AsistenciaDTO findById(Long id) {
        Asistencia asistencia = asistenciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asistencia not found with id: " + id));
        return mapEntityToDto(asistencia);
    }

    @Transactional
    public AsistenciaDTO createAsistencia(AsistenciaDTO asistenciaDTO) {
        // Verificar y establecer el estado de asistencia si es 0
        if (asistenciaDTO.getEstadoAsistenciaId() == 0) {
            asistenciaDTO.setEstadoAsistenciaId(ESTADO_PRESENTE);
        }

        Asistencia asistencia = mapDtoToEntity(asistenciaDTO);

        // Obtener la programación académica
        ProgramacionAcademica programacionAcademica = programacionAcademicaRepository.findById(asistenciaDTO.getProgramacionAcademicaId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid programacionAcademicaId: " + asistenciaDTO.getProgramacionAcademicaId()));

        // Verificar si el docente está autorizado para esta programación académica
        Docente docente = docenteRepository.findById(asistenciaDTO.getDocenteId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid docenteId: " + asistenciaDTO.getDocenteId()));

        if (!programacionAcademica.getDocentes().contains(docente)) {
            throw new IllegalArgumentException("El docente no está autorizado para esta programación académica.");
        }

        // Obtener el aula de la programación académica
        Aula aula = aulaRepository.findById(programacionAcademica.getAula().getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid aulaId: " + programacionAcademica.getAula().getId()));

        // Calcular la distancia entre la posición del docente y el aula
        double distance = GeoUtils.calculateDistance(asistenciaDTO.getLatitud(), asistenciaDTO.getLongitud(), aula.getLatitud(), aula.getLongitud());

        // Verificar si la distancia está dentro del rango permitido
        if (distance > MAX_DISTANCE) {
            throw new IllegalArgumentException("La posición del docente está fuera del rango permitido para registrar la asistencia.");
        }

        // Convertir la fecha y hora de asistenciaDTO a la zona horaria de Bolivia
        // Convertir la fecha y hora de asistenciaDTO a la zona horaria de Bolivia y aumentar 4 horas
        LocalDateTime registroHora = asistenciaDTO.getFecha().toInstant().atZone(ZoneId.of("America/La_Paz")).toLocalDateTime().plusHours(4);

// Formatear la fecha y hora para imprimir
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy HH:mm:ss", Locale.getDefault());
        String formattedDate = registroHora.format(formatter);

// Imprimir la fecha y hora
        System.out.println("Fecha y hora recibidas: " + formattedDate);
        boolean esDiaValido = programacionAcademica.getSesiones().stream().anyMatch(sesion ->
                sesion.getDiaSemana() == registroHora.getDayOfWeek() &&
                        !registroHora.toLocalTime().isBefore(sesion.getHoraInicio().minusMinutes(TOLERANCE_MINUTES))
        );

        if (!esDiaValido) {
            throw new IllegalArgumentException("La hora de registro no coincide con el horario de ninguna sesión de clase.");
        }

        // Determinar el estado de la asistencia basado en la hora de registro
        for (SesionClase sesion : programacionAcademica.getSesiones()) {
            if (sesion.getDiaSemana() == registroHora.getDayOfWeek()) {
                if (registroHora.toLocalTime().isBefore(sesion.getHoraInicio().plusMinutes(TOLERANCE_MINUTES))) {
                    asistencia.setEstadoAsistencia(estadoAsistenciaRepository.findById(ESTADO_PRESENTE).orElse(null));
                } else if (!registroHora.toLocalTime().isAfter(sesion.getHoraFin())) {
                    asistencia.setEstadoAsistencia(estadoAsistenciaRepository.findById(ESTADO_RETRASO).orElse(null));
                } else {
                    asistencia.setEstadoAsistencia(estadoAsistenciaRepository.findById(ESTADO_AUSENTE).orElse(null));
                }
                break;
            }
        }

        asistencia.setDocente(docente);

        return mapEntityToDto(asistenciaRepository.save(asistencia));
    }

    @Transactional
    public AsistenciaDTO updateAsistencia(Long id, AsistenciaDTO asistenciaDTO) {
        Asistencia existingAsistencia = asistenciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asistencia not found with id: " + id));
        updateEntityFromDto(existingAsistencia, asistenciaDTO);
        return mapEntityToDto(asistenciaRepository.save(existingAsistencia));
    }

    @Transactional
    public void deleteAsistencia(Long id) {
        Asistencia asistencia = asistenciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asistencia not found with id: " + id));
        asistenciaRepository.delete(asistencia);
    }

    private Asistencia mapDtoToEntity(AsistenciaDTO dto) {
        Asistencia asistencia = new Asistencia();
        asistencia.setProgramacionAcademica(programacionAcademicaRepository.findById(dto.getProgramacionAcademicaId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid programacionAcademicaId: " + dto.getProgramacionAcademicaId())));
        asistencia.setEstadoAsistencia(estadoAsistenciaRepository.findById(dto.getEstadoAsistenciaId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid estadoAsistenciaId: " + dto.getEstadoAsistenciaId())));
        asistencia.setDocente(docenteRepository.findById(dto.getDocenteId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid docenteId: " + dto.getDocenteId())));
        asistencia.setFecha(dto.getFecha());
        asistencia.setObservaciones(dto.getObservaciones());
        asistencia.setFotoUrl(dto.getFotoUrl());
        asistencia.setLatitud(dto.getLatitud());
        asistencia.setLongitud(dto.getLongitud());
        return asistencia;
    }

    private void updateEntityFromDto(Asistencia asistencia, AsistenciaDTO dto) {
        asistencia.setProgramacionAcademica(programacionAcademicaRepository.findById(dto.getProgramacionAcademicaId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid programacionAcademicaId: " + dto.getProgramacionAcademicaId())));
        asistencia.setEstadoAsistencia(estadoAsistenciaRepository.findById(dto.getEstadoAsistenciaId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid estadoAsistenciaId: " + dto.getEstadoAsistenciaId())));
        asistencia.setDocente(docenteRepository.findById(dto.getDocenteId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid docenteId: " + dto.getDocenteId())));
        asistencia.setFecha(dto.getFecha());
        asistencia.setObservaciones(dto.getObservaciones());
        asistencia.setFotoUrl(dto.getFotoUrl());
        asistencia.setLatitud(dto.getLatitud());
        asistencia.setLongitud(dto.getLongitud());
    }

    private AsistenciaDTO mapEntityToDto(Asistencia asistencia) {
        AsistenciaDTO dto = new AsistenciaDTO();
        dto.setId(asistencia.getId());
        dto.setProgramacionAcademicaId(asistencia.getProgramacionAcademica().getId());
        dto.setEstadoAsistenciaId(asistencia.getEstadoAsistencia().getId());
        dto.setDocenteId(asistencia.getDocente().getId());
        dto.setFecha(asistencia.getFecha());
        dto.setObservaciones(asistencia.getObservaciones());
        dto.setFotoUrl(asistencia.getFotoUrl());
        dto.setLatitud(asistencia.getLatitud());
        dto.setLongitud(asistencia.getLongitud());
        return dto;
    }

    public List<Asistencia> findByEstadoPresente() {
        return asistenciaRepository.findByEstadoAsistenciaId(ESTADO_PRESENTE);
    }

    public List<Asistencia> findByEstadoRetraso() {
        return asistenciaRepository.findByEstadoAsistenciaId(ESTADO_RETRASO);
    }

    public List<Asistencia> findByEstadoAusente() {
        return asistenciaRepository.findByEstadoAsistenciaId(ESTADO_AUSENTE);
    }
}
