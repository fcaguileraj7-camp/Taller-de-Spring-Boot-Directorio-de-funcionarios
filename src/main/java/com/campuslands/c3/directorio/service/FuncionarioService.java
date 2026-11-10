package com.campuslands.c3.directorio.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.campuslands.c3.directorio.entity.Funcionario;
import com.campuslands.c3.directorio.repository.FuncionarioRepository;

@Service
public class FuncionarioService {
    private final FuncionarioRepository funcionarioRepository;

    public FuncionarioService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    public List<Funcionario> listar(String nombre, String departamento) {
        // Si ambos filtros tienen valor, el taller da prioridad al nombre.
        if (nombre != null && !nombre.isBlank()) {
            return funcionarioRepository.findByNombreContainingIgnoreCase(nombre.strip());
        }
        if (departamento != null && !departamento.isBlank()) {
            return funcionarioRepository.findByDepartamentoIgnoreCase(departamento.strip());
        }
        return funcionarioRepository.findAll();
    }

    public Funcionario buscarPorId(Long id) {
        return funcionarioRepository.findById(id).orElse(null);
    }

    public Funcionario registrar(Funcionario funcionario) {
        if (funcionario.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID debe omitirse o ser nulo al registrar");
        }
        validarCampos(funcionario);
        return funcionarioRepository.save(funcionario);
    }

    @Transactional
    public Funcionario actualizar(Long id, Funcionario datos) {
        validarCampos(datos);
        Funcionario funcionario = buscarPorId(id);
        if (funcionario == null) {
            return null;
        }
        // El ID de la ruta identifica el registro y no cambia durante la edición.
        funcionario.setNombre(datos.getNombre());
        funcionario.setCargo(datos.getCargo());
        funcionario.setDepartamento(datos.getDepartamento());
        funcionario.setTelefono(datos.getTelefono());
        return funcionarioRepository.save(funcionario);
    }

    @Transactional
    public boolean eliminar(Long id) {
        Funcionario funcionario = buscarPorId(id);
        if (funcionario == null) {
            return false;
        }
        funcionarioRepository.delete(funcionario);
        return true;
    }

    private void validarCampos(Funcionario funcionario) {
        if (funcionario.getNombre() == null || funcionario.getNombre().isBlank()
                || funcionario.getCargo() == null || funcionario.getCargo().isBlank()
                || funcionario.getDepartamento() == null || funcionario.getDepartamento().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nombre, cargo y departamento son obligatorios y no pueden quedar en blanco");
        }
    }
}
