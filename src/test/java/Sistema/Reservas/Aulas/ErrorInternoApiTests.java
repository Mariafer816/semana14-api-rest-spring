package Sistema.Reservas.Aulas;

import Sistema.Reservas.Aulas.Service.AulaService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Un fallo inesperado en el servicio debe responder 500 con JSON y sin detalles internos. */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorInternoApiTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AulaService aulaService;

    @Test
    void falloInesperadoDevuelve500ConJson() throws Exception {
        Mockito.when(aulaService.consultarTodas()).thenThrow(new IllegalStateException("fallo simulado de la base de datos"));

        mockMvc.perform(get("/api/aulas"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.estado").value(500))
                .andExpect(jsonPath("$.mensaje").value("Error interno del servidor"));
    }
}
