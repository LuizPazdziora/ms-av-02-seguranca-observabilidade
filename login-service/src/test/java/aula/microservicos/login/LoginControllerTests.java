package aula.microservicos.login;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

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

    private static final RequestPostProcessor ADMIN = httpBasic("admin", "admin-teste");
    private static final RequestPostProcessor MANUTENCAO = httpBasic("manutencao", "manutencao-teste");

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

    private MockHttpServletRequestBuilder cadastro(String username, String senha) throws Exception {
        return post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CadastroUsuarioBean(username, senha)));
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
        mockMvc.perform(get("/").with(MANUTENCAO))
                .andExpect(jsonPath("$[?(@.username == 'joao')].bloqueado").value(true));
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
        // operadores nao sao usuarios do sistema generico
        login("admin", "admin-teste").andExpect(status().isUnauthorized());
    }

    // 4
    @Test
    void desbloqueioPeloAdminPermiteNovoLogin() throws Exception {
        bloquear("joao");
        mockMvc.perform(put("/unlock/joao").with(ADMIN)).andExpect(status().isOk());
        login("joao", SENHA_USUARIO).andExpect(status().isOk());
    }

    // 5
    @Test
    void desbloqueioExigeRoleAdmin() throws Exception {
        mockMvc.perform(put("/unlock/joao").with(MANUTENCAO)).andExpect(status().isForbidden());
        mockMvc.perform(put("/unlock/joao")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/unlock/joao").with(httpBasic("admin", "senha-errada"))).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/unlock/inexistente").with(ADMIN)).andExpect(status().isNotFound());
        // usuario do sistema (ROLE_USER) autentica, mas nao tem a role exigida
        mockMvc.perform(put("/unlock/joao").with(httpBasic("joao", SENHA_USUARIO))).andExpect(status().isForbidden());
    }

    // 6
    @Test
    void listarExigeRoleManutencaoENaoExpoeSenha() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/").with(ADMIN)).andExpect(status().isForbidden());
        mockMvc.perform(get("/").with(MANUTENCAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("joao"))
                .andExpect(jsonPath("$[1].username").value("maria"))
                .andExpect(jsonPath("$[0].senha").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(content().string(not(containsString("$2a$"))))
                .andExpect(content().string(not(containsString("admin"))));
    }

    // 7
    @Test
    void cadastroExigeRoleManutencao() throws Exception {
        mockMvc.perform(cadastro("pedro", "senha-pedro").with(MANUTENCAO)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("pedro"))
                .andExpect(jsonPath("$.bloqueado").value(false))
                .andExpect(jsonPath("$.senha").doesNotExist());
        mockMvc.perform(cadastro("pedro", "senha-pedro").with(MANUTENCAO)).andExpect(status().isConflict());
        mockMvc.perform(cadastro("ana", "senha-ana").with(ADMIN)).andExpect(status().isForbidden());
        mockMvc.perform(cadastro("ana", "senha-ana")).andExpect(status().isUnauthorized());
        mockMvc.perform(cadastro("admin", "outra-senha").with(MANUTENCAO)).andExpect(status().isConflict());
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
        mockMvc.perform(cadastro("", "senha").with(MANUTENCAO)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists());
        mockMvc.perform(cadastro("u".repeat(51), "senha").with(MANUTENCAO)).andExpect(status().isBadRequest());
        // 72 caracteres, mas mais de 72 bytes em UTF-8
        mockMvc.perform(cadastro("acentuado", "á".repeat(72)).with(MANUTENCAO)).andExpect(status().isBadRequest());
    }

}
