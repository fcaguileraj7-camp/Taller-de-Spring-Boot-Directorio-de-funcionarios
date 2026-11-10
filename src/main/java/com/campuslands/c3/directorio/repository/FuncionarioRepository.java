package com.campuslands.c3.directorio.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.campuslands.c3.directorio.entity.Funcionario;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    List<Funcionario> findByNombreContainingIgnoreCase(String nombre);

    List<Funcionario> findByDepartamentoIgnoreCase(String departamento);
}
