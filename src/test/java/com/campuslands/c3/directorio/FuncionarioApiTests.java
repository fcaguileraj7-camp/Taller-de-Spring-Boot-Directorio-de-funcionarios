package com.campuslands.c3.directorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.campuslands.c3.directorio.entity.Funcionario;
import com.campuslands.c3.directorio.repository.FuncionarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FuncionarioApiTests {
    private static final String API = "/api/v1/funcionarios";
    private static final String DATOS = """
            {"nombre":"Ana Pérez","cargo":"Analista","departamento":"Contabilidad"}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private FuncionarioRepository repository;

    @Test
    void listarVacioResponde204SinCuerpo() throws Exception {
        mvc.perform(get(API)).andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @Test
    void registrarGeneraIdYPermiteOmitirTelefono() throws Exception {
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Ana Pérez"));
        assertEquals(1, repository.count());
    }

    @Test
    void listarYConsultarDevuelvenDatosGuardados() throws Exception {
        Funcionario funcionario = guardar();
        mvc.perform(get(API)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(funcionario.getId()));
        mvc.perform(get(API + "/" + funcionario.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("001-234"));
    }

    @Test
    void actualizarConservaIdDeRutaYPermiteVaciarTelefono() throws Exception {
        Funcionario funcionario = guardar();
        mvc.perform(put(API + "/" + funcionario.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"id":99999,"nombre":"Ana Gómez","cargo":"Jefe",
                         "departamento":"Talento Humano","telefono":""}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(funcionario.getId()))
                .andExpect(jsonPath("$.nombre").value("Ana Gómez"))
                .andExpect(jsonPath("$.telefono").value(""));
        assertEquals(1, repository.count());
        assertEquals("Jefe", repository.findById(funcionario.getId()).orElseThrow().getCargo());
    }

    @Test
    void eliminarResponde204YRetiraElRegistro() throws Exception {
        Long id = guardar().getId();
        mvc.perform(delete(API + "/" + id)).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        assertFalse(repository.existsById(id));
        mvc.perform(get(API + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void idInexistenteResponde404EnConsultaActualizacionYEliminacion() throws Exception {
        mvc.perform(get(API + "/99999")).andExpect(status().isNotFound());
        mvc.perform(put(API + "/99999").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isNotFound());
        mvc.perform(delete(API + "/99999")).andExpect(status().isNotFound());
        assertEquals(0, repository.count());
    }

    private Funcionario guardar() {
        Funcionario funcionario = new Funcionario();
        funcionario.setNombre("Ana Pérez");
        funcionario.setCargo("Analista");
        funcionario.setDepartamento("Contabilidad");
        funcionario.setTelefono("001-234");
        return repository.saveAndFlush(funcionario);
    }

    @Test
    void crearConIdNuloEsValido() throws Exception {
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON)
                .content(DATOS.replace("{", "{\"id\":null,")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void crearConIdNoNuloResponde400SinSobrescribir() throws Exception {
        Funcionario existente = guardar();
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON)
                .content(DATOS.replace("{", "{\"id\":" + existente.getId() + ",")))
                .andExpect(status().isBadRequest());
        assertEquals(1, repository.count());
        assertEquals("001-234", repository.findById(existente.getId()).orElseThrow().getTelefono());
    }

    @ParameterizedTest
    @ValueSource(strings = {"nombre", "cargo", "departamento"})
    void camposObligatoriosFaltantesNulosVaciosOBlancosResponden400(String campo) throws Exception {
        Long id = guardar().getId();
        String otrosCampos = switch (campo) {
            case "nombre" -> "\"cargo\":\"Analista\",\"departamento\":\"Contabilidad\"";
            case "cargo" -> "\"nombre\":\"Ana\",\"departamento\":\"Contabilidad\"";
            default -> "\"nombre\":\"Ana\",\"cargo\":\"Analista\"";
        };
        for (String valor : new String[] {null, "null", "\"\"", "\"   \""}) {
            String cuerpo = "{" + otrosCampos + (valor == null ? "" : ",\"" + campo + "\":" + valor) + "}";
            mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest());
            mvc.perform(put(API + "/" + id).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(1, repository.count());
        assertEquals("Ana Pérez", repository.findById(id).orElseThrow().getNombre());
    }

    @Test
    void buscarNombreEsParcialIgnoraMayusculasYTienePrioridad() throws Exception {
        guardar();
        mvc.perform(get(API).param("nombre", "nA").param("departamento", "Otro"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Ana Pérez"));
    }

    @Test
    void departamentoEsExactoEIgnoraMayusculas() throws Exception {
        guardar();
        mvc.perform(get(API).param("departamento", "cONTABILIDAD"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(API).param("departamento", "Conta"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @Test
    void filtrosVaciosSeIgnoranYNombreEnBlancoPermiteDepartamento() throws Exception {
        guardar();
        mvc.perform(get(API).param("nombre", "").param("departamento", "   "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(API).param("nombre", "   ").param("departamento", "Contabilidad"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(API).param("nombre", "   ").param("departamento", "Otro"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @Test
    void buscarSinCoincidenciasDevuelve204SinCuerpo() throws Exception {
        guardar();
        mvc.perform(get(API).param("nombre", "inexistente"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @Test
    void jsonMalFormadoEIdNoNumericoResponden400() throws Exception {
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(API + "/texto")).andExpect(status().isBadRequest());
    }
}
