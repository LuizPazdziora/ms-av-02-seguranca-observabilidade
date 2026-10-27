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
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginBean login) {
        logger.debug("Tentativa de login do usuario = " + login.getUsername());
        if (loginServico.login(login.getUsername(), login.getSenha()))
            return new ResponseEntity<String>("Login efetuado com sucesso", HttpStatus.OK);
        return new ResponseEntity<String>("Usuario ou senha invalidos", HttpStatus.UNAUTHORIZED);
    }

    // PUT - http://localhost:8080/trocarsenha/joao
    @PutMapping("/trocarsenha/{userid}")
    public ResponseEntity<String> trocarSenha(@PathVariable String userid, @Valid @RequestBody TrocaSenhaBean troca) {
        logger.debug("Troca de senha do usuario = " + userid);
        loginServico.trocarSenha(userid, troca.getAtual(), troca.getNova());
        return new ResponseEntity<String>("Senha alterada com sucesso", HttpStatus.OK);
    }

    // POST - http://localhost:8080/
    @PostMapping("/")
    public ResponseEntity<UsuarioBean> cadastrar(@Valid @RequestBody CadastroUsuarioBean cadastro) {
        logger.debug("Cadastro do usuario = " + cadastro.getUsername());
        UsuarioBean usuario = loginServico.cadastrar(cadastro.getUsername(), cadastro.getSenha());
        return new ResponseEntity<UsuarioBean>(usuario, HttpStatus.CREATED);
    }

    // PUT - http://localhost:8080/unlock/joao
    @PutMapping("/unlock/{userid}")
    public ResponseEntity<String> desbloquear(@PathVariable String userid) {
        logger.debug("Desbloqueio do usuario = " + userid);
        loginServico.desbloquear(userid);
        return new ResponseEntity<String>("Usuario desbloqueado com sucesso", HttpStatus.OK);
    }

    // GET - http://localhost:8080/
    @GetMapping("/")
    public ResponseEntity<Iterable<UsuarioBean>> listar() {
        logger.debug("Listando usuarios cadastrados");
        return new ResponseEntity<Iterable<UsuarioBean>>(loginServico.listar(), HttpStatus.OK);
    }

}
