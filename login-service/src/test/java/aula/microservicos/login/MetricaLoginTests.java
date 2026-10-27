package aula.microservicos.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import aula.microservicos.login.bean.LoginBean;
import aula.microservicos.login.servico.LoginServico;
import io.micrometer.core.instrument.MeterRegistry;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MetricaLoginTests {

    private static final String SENHA_USUARIO = "usuario-teste";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private LoginServico loginServico;

    private ObjectMapper mapper = new ObjectMapper();

    private ResultActions login(String username, String senha) throws Exception {
        return mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginBean(username, senha))));
    }

    private double validos() {
        return registry.get("login.validos").counter().count();
    }

    private double invalidos() {
        return registry.get("login.invalidos").counter().count();
    }

    private double bloqueados() {
        return registry.get("usuarios.bloqueados").gauge().value();
    }

    @Test
    void metricasIniciamZeradas() {
        assertEquals(0.0, validos());
        assertEquals(0.0, invalidos());
        assertEquals(0.0, bloqueados());
    }

    @Test
    void contadoresAumentamComOsLogins() throws Exception {
        login("joao", SENHA_USUARIO).andExpect(status().isOk());
        login("maria", SENHA_USUARIO).andExpect(status().isOk());
        login("joao", "errada").andExpect(status().isUnauthorized());
        login("inexistente", "qualquer").andExpect(status().isUnauthorized());
        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        assertEquals(2.0, validos());
        assertEquals(3.0, invalidos());
    }

    @Test
    void gaugeAcompanhaBloqueioEDesbloqueio() throws Exception {
        login("joao", "errada-1").andExpect(status().isUnauthorized());
        assertEquals(0.0, bloqueados());
        login("joao", "errada-2").andExpect(status().isUnauthorized());
        assertEquals(1.0, bloqueados());

        // login negado de usuario bloqueado tambem conta como invalido
        login("joao", SENHA_USUARIO).andExpect(status().isUnauthorized());
        assertEquals(3.0, invalidos());

        loginServico.desbloquear("joao");
        assertEquals(0.0, bloqueados());
    }

}
