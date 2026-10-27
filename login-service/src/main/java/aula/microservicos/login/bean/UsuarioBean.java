package aula.microservicos.login.bean;

import io.swagger.v3.oas.annotations.media.Schema;

// representacao publica de um usuario: nunca contem a senha nem o hash
@Schema(name="Usuario", description = "Usuario cadastrado (nunca contem senha ou hash)")
public class UsuarioBean {

    @Schema(name = "username", description = "Username do usuario", example = "joao")
    private String username;

    @Schema(name = "bloqueado", description = "Indica se o usuario esta bloqueado", example = "false")
    private boolean bloqueado;

    @Schema(name = "tentativasFalhas", description = "Tentativas de login falhas consecutivas", example = "0")
    private int tentativasFalhas;

    public UsuarioBean() {
    }

    public UsuarioBean(String username, boolean bloqueado, int tentativasFalhas) {
        this.username = username;
        this.bloqueado = bloqueado;
        this.tentativasFalhas = tentativasFalhas;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isBloqueado() {
        return bloqueado;
    }

    public void setBloqueado(boolean bloqueado) {
        this.bloqueado = bloqueado;
    }

    public int getTentativasFalhas() {
        return tentativasFalhas;
    }

    public void setTentativasFalhas(int tentativasFalhas) {
        this.tentativasFalhas = tentativasFalhas;
    }

}
