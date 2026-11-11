package Sistema.Reservas.Aulas;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Pruebas de /api/aulas: códigos 200, 201 + Location, 204, 400 y 404 (API pública por defecto). */
@SpringBootTest
@AutoConfigureMockMvc
class AulaApiTests {

    private static final String JSON_VALIDO = """
            {"nombre":"API-101","capacidad":30,"ubicacion":"Edificio API","tipo":"Laboratorio","disponible":true}""";

    @Autowired
    private MockMvc mockMvc;

    private long crearAula() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/aulas")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    @Test
    void listarDevuelve200ConArregloJson() throws Exception {
        mockMvc.perform(get("/api/aulas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void postDevuelve201ConCabeceraLocationYSalidaConId() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/aulas")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("API-101"))
                .andReturn();
        long id = ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
        // La cabecera Location apunta al recurso recién creado
        mockMvc.perform(get("/api/aulas/" + id)).andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertTrue(
                r.getResponse().getHeader("Location").endsWith("/api/aulas/" + id));
    }

    @Test
    void elIdEnviadoEnElCuerpoSeIgnora() throws Exception {
        long id1 = crearAula();
        MvcResult r = mockMvc.perform(post("/api/aulas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + id1 + ",\"nombre\":\"OtraAula\",\"capacidad\":10,\"ubicacion\":\"X\",\"tipo\":\"Laboratorio\"}"))
                .andExpect(status().isCreated()).andReturn();
        long id2 = ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
        org.junit.jupiter.api.Assertions.assertNotEquals(id1, id2);
        mockMvc.perform(get("/api/aulas/" + id1)).andExpect(jsonPath("$.nombre").value("API-101"));
    }

    @Test
    void cicloCompletoCrearConsultarActualizarEliminar() throws Exception {
        long id = crearAula();

        mockMvc.perform(get("/api/aulas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("API-101"));

        mockMvc.perform(put("/api/aulas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"API-101B","capacidad":45,"ubicacion":"Edificio API 2","tipo":"Auditorio","disponible":false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombre").value("API-101B"))
                .andExpect(jsonPath("$.tipo").value("Auditorio"))
                .andExpect(jsonPath("$.disponible").value(false));

        mockMvc.perform(delete("/api/aulas/" + id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(emptyString()));

        mockMvc.perform(get("/api/aulas/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void postConDatosInvalidosDevuelve400ConDetalle() throws Exception {
        mockMvc.perform(post("/api/aulas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","capacidad":0,"ubicacion":"","tipo":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.capacidad").exists());
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/aulas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"X\", \"capacidad\": \"abc\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));
    }

    @Test
    void idNoNumericoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/aulas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));
    }

    @Test
    void idInexistenteDevuelve404EnGetPutDelete() throws Exception {
        mockMvc.perform(get("/api/aulas/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404));

        mockMvc.perform(put("/api/aulas/999999")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/aulas/999999")).andExpect(status().isNotFound());
    }

    @Test
    void metodoNoPermitidoConservaSuCodigoYNoSeConvierteEn500() throws Exception {
        mockMvc.perform(delete("/api/aulas")).andExpect(status().isMethodNotAllowed());
    }
}
