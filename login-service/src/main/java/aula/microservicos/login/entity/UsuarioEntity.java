package aula.microservicos.login.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="USUARIOS")
public class UsuarioEntity {
    @Id
    @Column(name = "USERNAME")
    private String username;
    @Column(name = "SENHA")
    private String senha;

    public UsuarioEntity() {
    }

    public UsuarioEntity(String username, String senha) {
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
