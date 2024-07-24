package com.example.examen2.programacionacademica.model;

import java.util.Set;

public class ProgramacionAcademicaDTO {
    private Long id;
    private Long materiaId;
    private Long aulaId;
    private Set<Long> docenteIds;
    private Set<Long> sesionClaseIds;
    private String grupo; // Nuevo atributo

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMateriaId() {
        return materiaId;
    }

    public void setMateriaId(Long materiaId) {
        this.materiaId = materiaId;
    }

    public Long getAulaId() {
        return aulaId;
    }

    public void setAulaId(Long aulaId) {
        this.aulaId = aulaId;
    }

    public Set<Long> getDocenteIds() {
        return docenteIds;
    }

    public void setDocenteIds(Set<Long> docenteIds) {
        this.docenteIds = docenteIds;
    }

    public Set<Long> getSesionClaseIds() {
        return sesionClaseIds;
    }

    public void setSesionClaseIds(Set<Long> sesionClaseIds) {
        this.sesionClaseIds = sesionClaseIds;
    }

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }
}
