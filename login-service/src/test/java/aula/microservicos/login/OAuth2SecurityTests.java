package aula.microservicos.login;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import aula.microservicos.login.bean.CadastroUsuarioBean;
import aula.microservicos.login.bean.LoginBean;
import tools.jackson.databind.ObjectMapper;

// profile oauth2: nao depende de um Keycloak em execucao (o token e simulado pelo jwt())
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "oauth2"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OAuth2SecurityTests {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/unlock/joao")).andExpect(status().isUnauthorized());
        // Basic nao e aceito no profile oauth2
        mockMvc.perform(get("/").with(httpBasic("manutencao", "manutencao-teste"))).andExpect(status().isUnauthorized());
    }

    @Test
    void roleErradaRetorna403() throws Exception {
        mockMvc.perform(get("/").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/unlock/joao").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MANUTENCAO"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void roleCertaRetorna200() throws Exception {
        mockMvc.perform(get("/").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MANUTENCAO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("joao"));
        mockMvc.perform(put("/unlock/joao").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MANUTENCAO")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CadastroUsuarioBean("pedro", "senha-pedro"))))
                .andExpect(status().isCreated());
    }

    @Test
    void endpointsPublicosContinuamPublicos() throws Exception {
        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginBean("joao", "usuario-teste"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.auth.type").value("oauth2"));
    }

}
