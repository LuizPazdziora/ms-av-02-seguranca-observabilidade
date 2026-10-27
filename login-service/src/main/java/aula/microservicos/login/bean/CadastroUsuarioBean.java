package aula.microservicos.login.bean;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CadastroUsuarioBean {

    // limite de 50 caracteres: users.username e varchar(50) no schema fixo do Spring Security
    @NotBlank(message = "Username deve ser informado")
    @Size(max = 50, message = "Username deve conter no maximo 50 caracteres")
    private String username;

    @NotBlank(message = "Senha deve ser informada")
    @Size(max = 72, message = "Senha deve conter no maximo 72 caracteres")
    private String senha;

    public CadastroUsuarioBean() {
    }

    public CadastroUsuarioBean(String username, String senha) {
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
