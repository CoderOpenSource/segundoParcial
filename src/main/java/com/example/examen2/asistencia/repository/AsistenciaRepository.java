package com.example.examen2.asistencia.repository;

import com.example.examen2.asistencia.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {
    List<Asistencia> findByEstadoAsistenciaId(Long estadoAsistenciaId);
}
