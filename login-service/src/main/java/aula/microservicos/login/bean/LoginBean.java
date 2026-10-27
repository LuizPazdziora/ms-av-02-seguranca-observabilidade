package aula.microservicos.login.bean;

import io.swagger.v3.oas.annotations.media.Schema;

// sem anotacoes de validacao: campos ausentes, vazios ou longos demais
// contam como login invalido (401), conforme o enunciado (somente 200 ou 401)
@Schema(name="Login", description = "Credenciais para o login (sem validacao: dados invalidos resultam em 401)")
public class LoginBean {

    @Schema(name = "username", description = "Username do usuario", example = "joao", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(name = "senha", description = "Senha do usuario", example = "usuario-demo", requiredMode = Schema.RequiredMode.REQUIRED)
    private String senha;

    public LoginBean() {
    }

    public LoginBean(String username, String senha) {
        this.username = username;
        this.senha = senha;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

}
