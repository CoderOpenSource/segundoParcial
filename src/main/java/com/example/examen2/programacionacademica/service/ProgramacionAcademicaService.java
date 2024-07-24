package com.example.examen2.programacionacademica.service;

import com.example.examen2.docente.model.Docente;
import com.example.examen2.docente.repository.DocenteRepository;
import com.example.examen2.materia.repository.MateriaRepository;
import com.example.examen2.modulo.repository.AulaRepository;
import com.example.examen2.programacionacademica.model.ProgramacionAcademica;
import com.example.examen2.programacionacademica.model.ProgramacionAcademicaDTO;
import com.example.examen2.programacionacademica.model.SesionClase;
import com.example.examen2.programacionacademica.repository.ProgramacionAcademicaRepository;
import com.example.examen2.programacionacademica.repository.SesionClaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProgramacionAcademicaService {

    @Autowired
    private ProgramacionAcademicaRepository programacionAcademicaRepository;

    @Autowired
    private MateriaRepository materiaRepository;

    @Autowired
    private AulaRepository aulaRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private SesionClaseRepository sesionClaseRepository;

    public List<ProgramacionAcademica> findAll() {
        return programacionAcademicaRepository.findAll();
    }

    @Transactional
    public ProgramacionAcademica createProgramacionAcademica(ProgramacionAcademicaDTO programacionAcademicaDTO) {
        ProgramacionAcademica programacionAcademica = mapDtoToEntity(programacionAcademicaDTO);
        return programacionAcademicaRepository.save(programacionAcademica);
    }

    @Transactional
    public ProgramacionAcademica updateProgramacionAcademica(Long id, ProgramacionAcademicaDTO programacionAcademicaDTO) {
        ProgramacionAcademica existingProgramacionAcademica = programacionAcademicaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Programacion Academica not found with id: " + id));
        updateEntityFromDto(existingProgramacionAcademica, programacionAcademicaDTO);
        return programacionAcademicaRepository.save(existingProgramacionAcademica);
    }

    @Transactional
    public void deleteProgramacionAcademica(Long id) {
        ProgramacionAcademica existingProgramacionAcademica = programacionAcademicaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Programacion Academica not found with id: " + id));

        // Eliminar relaciones con docentes antes de eliminar la programación académica
        existingProgramacionAcademica.getDocentes().forEach(docente -> docente.getProgramacionesAcademicas().remove(existingProgramacionAcademica));
        existingProgramacionAcademica.setDocentes(new HashSet<>());
        programacionAcademicaRepository.save(existingProgramacionAcademica);

        // Ahora podemos eliminar la programación académica
        programacionAcademicaRepository.delete(existingProgramacionAcademica);
    }

    private ProgramacionAcademica mapDtoToEntity(ProgramacionAcademicaDTO dto) {
        ProgramacionAcademica programacionAcademica = new ProgramacionAcademica();
        programacionAcademica.setMateria(materiaRepository.findById(dto.getMateriaId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid materiaId: " + dto.getMateriaId())));
        programacionAcademica.setAula(aulaRepository.findById(dto.getAulaId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid aulaId: " + dto.getAulaId())));

        if (dto.getDocenteIds() != null && !dto.getDocenteIds().isEmpty()) {
            Set<Docente> docentes = new HashSet<>(docenteRepository.findAllById(dto.getDocenteIds()));
            programacionAcademica.setDocentes(docentes);
        } else {
            programacionAcademica.setDocentes(new HashSet<>());
        }

        if (dto.getSesionClaseIds() != null && !dto.getSesionClaseIds().isEmpty()) {
            Set<SesionClase> sesiones = new HashSet<>(sesionClaseRepository.findAllById(dto.getSesionClaseIds()));
            programacionAcademica.setSesiones(sesiones);
        } else {
            programacionAcademica.setSesiones(new HashSet<>());
        }

        programacionAcademica.setGrupo(dto.getGrupo());

        return programacionAcademica;
    }

    private void updateEntityFromDto(ProgramacionAcademica programacionAcademica, ProgramacionAcademicaDTO dto) {
        programacionAcademica.setMateria(materiaRepository.findById(dto.getMateriaId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid materiaId: " + dto.getMateriaId())));
        programacionAcademica.setAula(aulaRepository.findById(dto.getAulaId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid aulaId: " + dto.getAulaId())));

        if (dto.getDocenteIds() != null && !dto.getDocenteIds().isEmpty()) {
            Set<Docente> docentes = new HashSet<>(docenteRepository.findAllById(dto.getDocenteIds()));
            programacionAcademica.setDocentes(docentes);
        } else {
            programacionAcademica.setDocentes(new HashSet<>());
        }

        if (dto.getSesionClaseIds() != null && !dto.getSesionClaseIds().isEmpty()) {
            Set<SesionClase> sesiones = new HashSet<>(sesionClaseRepository.findAllById(dto.getSesionClaseIds()));
            programacionAcademica.setSesiones(sesiones);
        } else {
            programacionAcademica.setSesiones(new HashSet<>());
        }

        programacionAcademica.setGrupo(dto.getGrupo());
    }
}
