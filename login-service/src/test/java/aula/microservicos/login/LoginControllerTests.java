package aula.microservicos.login;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

import aula.microservicos.login.bean.CadastroUsuarioBean;
import aula.microservicos.login.bean.LoginBean;
import aula.microservicos.login.bean.TrocaSenhaBean;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LoginControllerTests {

    private static final String SENHA_USUARIO = "usuario-teste";

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper mapper = new ObjectMapper();

    private ResultActions login(String username, String senha) throws Exception {
        return mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginBean(username, senha))));
    }

    private ResultActions trocarSenha(String userid, String atual, String nova) throws Exception {
        return mockMvc.perform(put("/trocarsenha/" + userid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new TrocaSenhaBean(atual, nova))));
    }

    private ResultActions cadastrar(String username, String senha) throws Exception {
        return mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CadastroUsuarioBean(username, senha))));
    }

    private void bloquear(String username) throws Exception {
        login(username, "errada-1").andExpect(status().isUnauthorized());
        login(username, "errada-2").andExpect(status().isUnauthorized());
    }

    // 1
    @Test
    void loginValido() throws Exception {
        login("joao", SENHA_USUARIO).andExpect(status().isOk());
    }

    // 2
    @Test
    void segundaFalhaBloqueiaUsuario() throws Exception {
        login("joao", "errada").andExpect(status().isUnauthorized());
        login("joao", "errada").andExpect(status().isUnauthorized());
        // continua negado mesmo com a senha correta
        login("joao", SENHA_USUARIO).andExpect(status().isUnauthorized());
    }

    @Test
    void loginCorretoZeraContadorAntesDoBloqueio() throws Exception {
        login("joao", "errada").andExpect(status().isUnauthorized());
        login("joao", SENHA_USUARIO).andExpect(status().isOk());
        login("joao", "errada").andExpect(status().isUnauthorized());
        login("joao", SENHA_USUARIO).andExpect(status().isOk());
    }

    // 3
    @Test
    void loginInvalidoRetorna401() throws Exception {
        login("inexistente", SENHA_USUARIO).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        login("joao", "a".repeat(73)).andExpect(status().isUnauthorized());
        // 72 caracteres acentuados = 144 bytes em UTF-8
        login("maria", "á".repeat(72)).andExpect(status().isUnauthorized());
    }

    // 4
    @Test
    void desbloqueioPermiteNovoLogin() throws Exception {
        bloquear("joao");
        mockMvc.perform(put("/unlock/joao")).andExpect(status().isOk());
        login("joao", SENHA_USUARIO).andExpect(status().isOk());
    }

    // 5
    @Test
    void desbloqueioUsuarioInexistente() throws Exception {
        mockMvc.perform(put("/unlock/inexistente")).andExpect(status().isNotFound());
    }

    // 6
    @Test
    void listarNaoExpoeSenha() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("joao"))
                .andExpect(jsonPath("$[1].username").value("maria"))
                .andExpect(jsonPath("$[0].senha").doesNotExist())
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    // 7
    @Test
    void cadastroDeUsuario() throws Exception {
        cadastrar("pedro", "senha-pedro").andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("pedro"))
                .andExpect(jsonPath("$.bloqueado").value(false));
        cadastrar("pedro", "senha-pedro").andExpect(status().isConflict());
        login("pedro", "senha-pedro").andExpect(status().isOk());
    }

    // 8
    @Test
    void trocaDeSenha() throws Exception {
        trocarSenha("joao", "errada", "nova-senha").andExpect(status().isUnauthorized());
        trocarSenha("joao", SENHA_USUARIO, "nova-senha").andExpect(status().isOk());
        login("joao", "nova-senha").andExpect(status().isOk());
        login("joao", SENHA_USUARIO).andExpect(status().isUnauthorized());
    }

    // 9
    @Test
    void trocaDeSenhaUsuarioInexistenteOuBloqueado() throws Exception {
        trocarSenha("inexistente", SENHA_USUARIO, "nova-senha").andExpect(status().isNotFound());
        bloquear("joao");
        trocarSenha("joao", SENHA_USUARIO, "nova-senha").andExpect(status().isUnauthorized());
    }

    // 10
    @Test
    void validacaoDosCampos() throws Exception {
        trocarSenha("joao", SENHA_USUARIO, "").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nova").exists());
        trocarSenha("joao", SENHA_USUARIO, "a".repeat(73)).andExpect(status().isBadRequest());
        cadastrar("", "senha").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists());
        cadastrar("u".repeat(51), "senha").andExpect(status().isBadRequest());
        // 72 caracteres, mas mais de 72 bytes em UTF-8
        cadastrar("acentuado", "á".repeat(72)).andExpect(status().isBadRequest());
    }

}
