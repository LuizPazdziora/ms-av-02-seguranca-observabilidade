package aula.microservicos.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import aula.microservicos.login.bean.LoginBean;
import aula.microservicos.login.servico.LoginServico;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SincronizacaoUsuarioTests {

    private static final String SENHA_USUARIO = "usuario-teste";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginServico loginServico;

    private ObjectMapper mapper = new ObjectMapper();

    private String hashUsuarios(String username) {
        return jdbc.queryForObject("select senha from usuarios where username = ?", String.class, username);
    }

    private String hashUsers(String username) {
        return jdbc.queryForObject("select password from users where username = ?", String.class, username);
    }

    private boolean habilitado(String username) {
        return jdbc.queryForObject("select enabled from users where username = ?", Boolean.class, username);
    }

    private boolean bloqueado(String username) {
        return jdbc.queryForObject("select locked from login_attempts where username = ?", Boolean.class, username);
    }

    private List<String> authorities(String username) {
        return jdbc.queryForList("select authority from authorities where username = ?", String.class, username);
    }

    // regra de consistencia entre usuarios/login_attempts e users/authorities
    private void verificarConsistencia(String username, String senhaEsperada) {
        String hash = hashUsuarios(username);
        assertEquals(hash, hashUsers(username));
        assertNotEquals(senhaEsperada, hash);
        assertTrue(hash.startsWith("$2a$"));
        assertTrue(passwordEncoder.matches(senhaEsperada, hash));
        assertEquals(List.of("ROLE_USER"), authorities(username));
        assertEquals(!bloqueado(username), habilitado(username));
    }

    private void login(String username, String senha) throws Exception {
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginBean(username, senha))));
    }

    @Test
    void cargaInicialConsistente() {
        verificarConsistencia("joao", SENHA_USUARIO);
        verificarConsistencia("maria", SENHA_USUARIO);
        assertEquals(List.of("ROLE_ADMIN"), authorities("admin"));
        assertEquals(List.of("ROLE_MANUTENCAO"), authorities("manutencao"));
        // operadores existem somente em users/authorities
        assertEquals(0, jdbc.queryForObject("select count(*) from usuarios where username in ('admin','manutencao')", Integer.class));
    }

    @Test
    void cadastroTrocaBloqueioEDesbloqueioMantemConsistencia() throws Exception {
        loginServico.cadastrar("pedro", "senha-pedro");
        verificarConsistencia("pedro", "senha-pedro");

        loginServico.trocarSenha("pedro", "senha-pedro", "senha-nova");
        verificarConsistencia("pedro", "senha-nova");

        login("pedro", "errada-1");
        login("pedro", "errada-2");
        assertTrue(bloqueado("pedro"));
        assertFalse(habilitado("pedro"));
        verificarConsistencia("pedro", "senha-nova");

        loginServico.desbloquear("pedro");
        assertFalse(bloqueado("pedro"));
        assertTrue(habilitado("pedro"));
        verificarConsistencia("pedro", "senha-nova");
    }

    @Test
    void nenhumaSenhaEmTextoPuro() throws Exception {
        loginServico.cadastrar("pedro", "senha-pedro");
        List<Map<String, Object>> senhas = jdbc.queryForList(
                "select senha as valor from usuarios union all select password as valor from users");
        for (Map<String, Object> linha : senhas) {
            String valor = (String) linha.get("VALOR");
            assertTrue(valor.startsWith("$2a$"), "valor nao esta em BCrypt");
            assertFalse(List.of(SENHA_USUARIO, "senha-pedro", "admin-teste", "manutencao-teste").contains(valor));
        }
    }

    @Test
    void jdbcUserDetailsManagerEnxergaACredencialDoNovoUsuario() throws Exception {
        loginServico.cadastrar("pedro", "senha-pedro");
        // autentica via Basic com a senha cadastrada (ROLE_USER -> 403, nao 401)
        mockMvc.perform(get("/").with(httpBasic("pedro", "senha-pedro"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/").with(httpBasic("pedro", "errada"))).andExpect(status().isUnauthorized());

        loginServico.trocarSenha("pedro", "senha-pedro", "senha-nova");
        mockMvc.perform(get("/").with(httpBasic("pedro", "senha-nova"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/").with(httpBasic("pedro", "senha-pedro"))).andExpect(status().isUnauthorized());

        // usuario bloqueado fica desabilitado tambem para o Spring Security
        login("pedro", "errada-1");
        login("pedro", "errada-2");
        mockMvc.perform(get("/").with(httpBasic("pedro", "senha-nova"))).andExpect(status().isUnauthorized());
    }

}
