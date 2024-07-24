package com.example.examen2.asistencia.controller;

import com.example.examen2.asistencia.model.AsistenciaDTO;
import com.example.examen2.asistencia.service.AsistenciaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/asistencias/")
public class AsistenciaController {

    @Autowired
    private AsistenciaService asistenciaService;

    @GetMapping("/")
    public List<AsistenciaDTO> getAllAsistencias() {
        return asistenciaService.findAll();
    }

    @GetMapping("/{id}")
    public AsistenciaDTO getAsistenciaById(@PathVariable Long id) {
        return asistenciaService.findById(id);
    }

    @PostMapping("/")
    public AsistenciaDTO createAsistencia(@RequestBody AsistenciaDTO asistenciaDTO) {
        return asistenciaService.createAsistencia(asistenciaDTO);
    }

    @PutMapping("/{id}")
    public AsistenciaDTO updateAsistencia(@PathVariable Long id, @RequestBody AsistenciaDTO asistenciaDTO) {
        return asistenciaService.updateAsistencia(id, asistenciaDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteAsistencia(@PathVariable Long id) {
        asistenciaService.deleteAsistencia(id);
    }
}
