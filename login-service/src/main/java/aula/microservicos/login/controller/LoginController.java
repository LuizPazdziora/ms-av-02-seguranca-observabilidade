package aula.microservicos.login.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import aula.microservicos.login.bean.CadastroUsuarioBean;
import aula.microservicos.login.bean.LoginBean;
import aula.microservicos.login.bean.TrocaSenhaBean;
import aula.microservicos.login.bean.UsuarioBean;
import aula.microservicos.login.servico.LoginServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

// sem prefixo de @RequestMapping: o enunciado usa os caminhos na raiz
@RestController
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private LoginServico loginServico = null;

    public LoginController(LoginServico loginServico) {
        this.loginServico = loginServico;
    }

    // POST - http://localhost:8080/login
    @Operation(summary = "Efetua login", description = "Valida usuario e senha na tabela usuarios. Bloqueia o usuario na 2a falha seguida. Acesso publico.", tags = { "login" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario e senha validos", content = {
                    @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)) }),
            @ApiResponse(responseCode = "401", description = "Usuario ou senha invalidos, ou usuario bloqueado", content = {
                    @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)) }) })
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginBean login) {
        logger.debug("Tentativa de login do usuario = " + login.getUsername());
        if (loginServico.login(login.getUsername(), login.getSenha()))
            return new ResponseEntity<String>("Login efetuado com sucesso", HttpStatus.OK);
        return new ResponseEntity<String>("Usuario ou senha invalidos", HttpStatus.UNAUTHORIZED);
    }

    // PUT - http://localhost:8080/trocarsenha/joao
    @Operation(summary = "Troca a senha", description = "Troca a senha do usuario informando a senha atual e a nova. Acesso publico.", tags = { "login" })
    @Parameters(value={@Parameter(name = "userid", description = "Username do usuario", in = ParameterIn.PATH, required = true, example = "joao")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Senha alterada", content = {
                    @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)) }),
            @ApiResponse(responseCode = "400", description = "Dados invalidos (nova senha vazia ou com mais de 72 caracteres/bytes)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Senha atual invalida ou usuario bloqueado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario nao localizado", content = @Content) })
    @PutMapping("/trocarsenha/{userid}")
    public ResponseEntity<String> trocarSenha(@PathVariable String userid, @Valid @RequestBody TrocaSenhaBean troca) {
        logger.debug("Troca de senha do usuario = " + userid);
        loginServico.trocarSenha(userid, troca.getAtual(), troca.getNova());
        return new ResponseEntity<String>("Senha alterada com sucesso", HttpStatus.OK);
    }

    // POST - http://localhost:8080/
    @Operation(summary = "Cadastra usuario", description = "Cadastra um novo usuario do sistema. Exige a role ROLE_MANUTENCAO.", tags = { "usuarios" })
    @SecurityRequirement(name = "auth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario cadastrado", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioBean.class)) }),
            @ApiResponse(responseCode = "400", description = "Dados invalidos (username ou senha vazios ou longos demais)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Nao autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Autenticado sem a role ROLE_MANUTENCAO", content = @Content),
            @ApiResponse(responseCode = "409", description = "Usuario ja cadastrado", content = @Content) })
    @PostMapping("/")
    public ResponseEntity<UsuarioBean> cadastrar(@Valid @RequestBody CadastroUsuarioBean cadastro) {
        logger.debug("Cadastro do usuario = " + cadastro.getUsername());
        UsuarioBean usuario = loginServico.cadastrar(cadastro.getUsername(), cadastro.getSenha());
        return new ResponseEntity<UsuarioBean>(usuario, HttpStatus.CREATED);
    }

    // PUT - http://localhost:8080/unlock/joao
    @Operation(summary = "Desbloqueia usuario", description = "Desbloqueia o usuario e zera as tentativas falhas. Exige a role ROLE_ADMIN.", tags = { "usuarios" })
    @SecurityRequirement(name = "auth")
    @Parameters(value={@Parameter(name = "userid", description = "Username do usuario a ser desbloqueado", in = ParameterIn.PATH, required = true, example = "joao")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario desbloqueado", content = {
                    @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)) }),
            @ApiResponse(responseCode = "401", description = "Nao autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Autenticado sem a role ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario nao localizado", content = @Content) })
    @PutMapping("/unlock/{userid}")
    public ResponseEntity<String> desbloquear(@PathVariable String userid) {
        logger.debug("Desbloqueio do usuario = " + userid);
        loginServico.desbloquear(userid);
        return new ResponseEntity<String>("Usuario desbloqueado com sucesso", HttpStatus.OK);
    }

    // GET - http://localhost:8080/
    @Operation(summary = "Lista usuarios", description = "Retorna todos os usuarios cadastrados (sem senha). Exige a role ROLE_MANUTENCAO.", tags = { "usuarios" })
    @SecurityRequirement(name = "auth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuarios", content = {
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UsuarioBean.class))) }),
            @ApiResponse(responseCode = "401", description = "Nao autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Autenticado sem a role ROLE_MANUTENCAO", content = @Content) })
    @GetMapping("/")
    public ResponseEntity<Iterable<UsuarioBean>> listar() {
        logger.debug("Listando usuarios cadastrados");
        return new ResponseEntity<Iterable<UsuarioBean>>(loginServico.listar(), HttpStatus.OK);
    }

}
