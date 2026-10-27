package aula.microservicos.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import aula.microservicos.login.bean.CadastroUsuarioBean;
import aula.microservicos.login.bean.LoginBean;
import aula.microservicos.login.bean.TrocaSenhaBean;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class BeanValidacaoTests {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void criarValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fecharValidador() {
        factory.close();
    }

    private <T> Set<String> camposInvalidos(T bean) {
        Set<String> campos = new HashSet<String>();
        for (ConstraintViolation<T> violacao : validator.validate(bean)) {
            campos.add(violacao.getPropertyPath().toString());
        }
        return campos;
    }

    @Test
    void loginBeanNaoPossuiValidacao() {
        assertTrue(camposInvalidos(new LoginBean(null, null)).isEmpty());
        assertTrue(camposInvalidos(new LoginBean("", "a".repeat(100))).isEmpty());
    }

    @Test
    void trocaSenhaValida() {
        assertTrue(camposInvalidos(new TrocaSenhaBean("atual", "a".repeat(72))).isEmpty());
    }

    @Test
    void trocaSenhaAtualObrigatoria() {
        assertEquals(Set.of("atual"), camposInvalidos(new TrocaSenhaBean(null, "nova")));
        assertEquals(Set.of("atual"), camposInvalidos(new TrocaSenhaBean("  ", "nova")));
    }

    @Test
    void trocaSenhaNovaObrigatoriaEComLimite() {
        assertEquals(Set.of("nova"), camposInvalidos(new TrocaSenhaBean("atual", null)));
        assertEquals(Set.of("nova"), camposInvalidos(new TrocaSenhaBean("atual", "")));
        assertEquals(Set.of("nova"), camposInvalidos(new TrocaSenhaBean("atual", "a".repeat(73))));
    }

    @Test
    void cadastroValido() {
        assertTrue(camposInvalidos(new CadastroUsuarioBean("u".repeat(50), "s".repeat(72))).isEmpty());
    }

    @Test
    void cadastroUsernameObrigatorioEComLimite() {
        assertEquals(Set.of("username"), camposInvalidos(new CadastroUsuarioBean(null, "senha")));
        assertEquals(Set.of("username"), camposInvalidos(new CadastroUsuarioBean(" ", "senha")));
        assertEquals(Set.of("username"), camposInvalidos(new CadastroUsuarioBean("u".repeat(51), "senha")));
    }

    @Test
    void cadastroSenhaObrigatoriaEComLimite() {
        assertEquals(Set.of("senha"), camposInvalidos(new CadastroUsuarioBean("pedro", null)));
        assertEquals(Set.of("senha"), camposInvalidos(new CadastroUsuarioBean("pedro", "")));
        assertEquals(Set.of("senha"), camposInvalidos(new CadastroUsuarioBean("pedro", "s".repeat(73))));
    }

}
