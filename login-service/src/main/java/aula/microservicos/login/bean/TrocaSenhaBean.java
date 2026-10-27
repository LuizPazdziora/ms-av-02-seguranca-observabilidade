package aula.microservicos.login.bean;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TrocaSenhaBean {

    @NotBlank(message = "Senha atual deve ser informada")
    private String atual;

    @NotBlank(message = "Nova senha deve ser informada")
    @Size(max = 72, message = "Senha deve conter no maximo 72 caracteres")
    private String nova;

    public TrocaSenhaBean() {
    }

    public TrocaSenhaBean(String atual, String nova) {
        this.atual = atual;
        this.nova = nova;
    }

    public String getAtual() {
        return atual;
    }

    public void setAtual(String atual) {
        this.atual = atual;
    }

    public String getNova() {
        return nova;
    }

    public void setNova(String nova) {
        this.nova = nova;
    }

}
