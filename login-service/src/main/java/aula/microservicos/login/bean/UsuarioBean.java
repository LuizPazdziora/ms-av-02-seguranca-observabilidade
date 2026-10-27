package aula.microservicos.login.bean;

// representacao publica de um usuario: nunca contem a senha nem o hash
public class UsuarioBean {

    private String username;

    private boolean bloqueado;

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
