package aula.microservicos.login.bean;

// sem anotacoes de validacao: campos ausentes, vazios ou longos demais
// contam como login invalido (401), conforme o enunciado (somente 200 ou 401)
public class LoginBean {

    private String username;

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
