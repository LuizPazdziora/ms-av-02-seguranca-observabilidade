package aula.microservicos.login.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import aula.microservicos.login.bean.LoginBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

// FLUXO LEGADO DAS AULAS - SOMENTE DEMONSTRACAO LOCAL (desligado por padrao)
// O Resource Owner Password Credentials (grant_type=password) e proibido pela RFC 9700
// e nao existe no OAuth 2.1: a aplicacao passa a manipular a senha do usuario do Keycloak.
@RestController
@Profile("oauth2")
@ConditionalOnProperty(name = "seguranca.token-legado.habilitado", havingValue = "true")
public class TokenController {

    private static final Logger logger = LoggerFactory.getLogger(TokenController.class);

    @Value("${seguranca.token-legado.url}")
    private String tokenUrl;

    @Value("${seguranca.token-legado.client-id}")
    private String clientId;

    private RestClient restClient = RestClient.create();

    // POST - http://localhost:8080/token (usuario e senha no corpo, nunca na URL)
    @Operation(summary = "Obtem token (fluxo legado)", description = "Demonstracao local do grant_type=password das aulas. Proibido pela RFC 9700; use Authorization Code + PKCE.", tags = { "login" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token emitido pelo Keycloak", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas", content = @Content) })
    @PostMapping("/token")
    public ResponseEntity<String> token(@RequestBody LoginBean login) {

        // nunca registrar a senha nem o corpo da resposta (contem o token)
        logger.debug("Solicitacao de token (fluxo legado) para usuario = " + login.getUsername());

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<String, String>();
        requestBody.add("username", login.getUsername());
        requestBody.add("password", login.getSenha());
        requestBody.add("client_id", clientId);
        requestBody.add("grant_type", "password");

        String resposta = restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<String>(resposta, headers, HttpStatus.OK);
    }

}
