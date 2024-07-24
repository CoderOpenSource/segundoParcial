package com.example.examen2.programacionacademica.controller;

import com.example.examen2.programacionacademica.model.ProgramacionAcademica;
import com.example.examen2.programacionacademica.model.ProgramacionAcademicaDTO;
import com.example.examen2.programacionacademica.service.ProgramacionAcademicaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.examen2.docente.model.Docente;
import com.example.examen2.programacionacademica.model.SesionClase;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/programacionesacademicas/")
public class ProgramacionAcademicaController {

    @Autowired
    private ProgramacionAcademicaService programacionAcademicaService;

    @GetMapping("/")
    public List<ProgramacionAcademicaDTO> getAllProgramacionesAcademicas() {
        return programacionAcademicaService.findAll().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @PostMapping("/")
    public ProgramacionAcademicaDTO createProgramacionAcademica(@RequestBody ProgramacionAcademicaDTO programacionAcademicaDTO) {
        ProgramacionAcademica programacionAcademica = programacionAcademicaService.createProgramacionAcademica(programacionAcademicaDTO);
        return mapEntityToDto(programacionAcademica);
    }

    @PutMapping("/{id}")
    public ProgramacionAcademicaDTO updateProgramacionAcademica(@PathVariable Long id, @RequestBody ProgramacionAcademicaDTO programacionAcademicaDTO) {
        ProgramacionAcademica programacionAcademica = programacionAcademicaService.updateProgramacionAcademica(id, programacionAcademicaDTO);
        return mapEntityToDto(programacionAcademica);
    }

    @DeleteMapping("/{id}")
    public void deleteProgramacionAcademica(@PathVariable Long id) {
        programacionAcademicaService.deleteProgramacionAcademica(id);
    }

    private ProgramacionAcademicaDTO mapEntityToDto(ProgramacionAcademica programacionAcademica) {
        ProgramacionAcademicaDTO dto = new ProgramacionAcademicaDTO();
        dto.setId(programacionAcademica.getId());
        dto.setMateriaId(programacionAcademica.getMateria().getId());
        dto.setAulaId(programacionAcademica.getAula().getId());
        dto.setDocenteIds(programacionAcademica.getDocentes() != null ?
                programacionAcademica.getDocentes().stream().map(Docente::getId).collect(Collectors.toSet()) :
                new HashSet<>());
        dto.setSesionClaseIds(programacionAcademica.getSesiones() != null ?
                programacionAcademica.getSesiones().stream().map(SesionClase::getId).collect(Collectors.toSet()) :
                new HashSet<>());
        dto.setGrupo(programacionAcademica.getGrupo());
        return dto;
    }
}
