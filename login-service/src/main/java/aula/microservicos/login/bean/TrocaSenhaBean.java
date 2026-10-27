package aula.microservicos.login.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name="TrocaSenha", description = "Dados para a troca de senha")
public class TrocaSenhaBean {

    @NotBlank(message = "Senha atual deve ser informada")
    @Schema(name = "atual", description = "Senha atual", example = "usuario-demo", requiredMode = Schema.RequiredMode.REQUIRED)
    private String atual;

    @NotBlank(message = "Nova senha deve ser informada")
    @Size(max = 72, message = "Senha deve conter no maximo 72 caracteres")
    @Schema(name = "nova", description = "Nova senha (maximo 72 caracteres/bytes)", example = "nova-senha-demo", maxLength = 72, requiredMode = Schema.RequiredMode.REQUIRED)
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
