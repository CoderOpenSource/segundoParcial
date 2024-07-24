package com.example.examen2.asistencia.service;

import com.example.examen2.asistencia.model.Licencia;
import com.example.examen2.asistencia.model.LicenciaDTO;
import com.example.examen2.asistencia.repository.LicenciaRepository;
import com.example.examen2.config.cloudinary.CloudinaryService;
import com.example.examen2.docente.model.Docente;
import com.example.examen2.docente.repository.DocenteRepository;
import com.example.examen2.programacionacademica.model.ProgramacionAcademica;
import com.example.examen2.programacionacademica.repository.ProgramacionAcademicaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LicenciaService {

    @Autowired
    private LicenciaRepository licenciaRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private ProgramacionAcademicaRepository programacionAcademicaRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    public List<LicenciaDTO> findAll() {
        return licenciaRepository.findAll().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    public LicenciaDTO findById(Long id) {
        Licencia licencia = licenciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Licencia not found with id: " + id));
        return mapEntityToDto(licencia);
    }

    public LicenciaDTO createOrUpdateLicencia(LicenciaDTO licenciaDTO, MultipartFile file) throws IOException {
        ProgramacionAcademica programacionAcademica = programacionAcademicaRepository.findById(licenciaDTO.getProgramacionAcademicaId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid programacionAcademicaId: " + licenciaDTO.getProgramacionAcademicaId()));

        Set<Docente> docentes = programacionAcademica.getDocentes();
        Docente docente = docenteRepository.findById(licenciaDTO.getDocenteId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid docenteId: " + licenciaDTO.getDocenteId()));

        // Validación para asegurarse de que el docente esté en la programación académica
        if (!docentes.contains(docente)) {
            throw new IllegalArgumentException("El docente no está asociado con la programación académica especificada.");
        }

        Licencia licencia;
        if (licenciaDTO.getId() != null) {
            licencia = licenciaRepository.findById(licenciaDTO.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Licencia not found with id: " + licenciaDTO.getId()));
            updateEntityFromDto(licencia, licenciaDTO);
        } else {
            licencia = mapDtoToEntity(licenciaDTO);
        }

        // Subir archivo a Cloudinary
        if (file != null && !file.isEmpty()) {
            Map uploadResult = cloudinaryService.upload(file);
            licencia.setFotoLicencia((String) uploadResult.get("url"));
        }

        licencia = licenciaRepository.save(licencia);
        return mapEntityToDto(licencia);
    }

    public void deleteLicencia(Long id) {
        Licencia licencia = licenciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Licencia not found with id: " + id));
        licenciaRepository.delete(licencia);
    }

    private Licencia mapDtoToEntity(LicenciaDTO dto) {
        Licencia licencia = new Licencia();
        licencia.setDocente(docenteRepository.findById(dto.getDocenteId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid docenteId: " + dto.getDocenteId())));
        licencia.setProgramacionAcademica(programacionAcademicaRepository.findById(dto.getProgramacionAcademicaId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid programacionAcademicaId: " + dto.getProgramacionAcademicaId())));
        licencia.setFecha(dto.getFecha());
        licencia.setMotivo(dto.getMotivo());
        licencia.setFotoLicencia(dto.getFotoLicencia());
        return licencia;
    }

    private void updateEntityFromDto(Licencia licencia, LicenciaDTO dto) {
        licencia.setFecha(dto.getFecha());
        licencia.setMotivo(dto.getMotivo());
        licencia.setProgramacionAcademica(programacionAcademicaRepository.findById(dto.getProgramacionAcademicaId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid programacionAcademicaId: " + dto.getProgramacionAcademicaId())));
        licencia.setFotoLicencia(dto.getFotoLicencia());
    }

    private LicenciaDTO mapEntityToDto(Licencia licencia) {
        LicenciaDTO dto = new LicenciaDTO();
        dto.setId(licencia.getId());
        dto.setDocenteId(licencia.getDocente().getId());
        dto.setProgramacionAcademicaId(licencia.getProgramacionAcademica().getId());
        dto.setFecha(licencia.getFecha());
        dto.setMotivo(licencia.getMotivo());
        dto.setFotoLicencia(licencia.getFotoLicencia());
        return dto;
    }
}
