package aula.microservicos.login.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(
            value = HttpStatus.NOT_FOUND,
            reason = "Usuário não localizado!")
    @ExceptionHandler(UsuarioNaoLocalizadoException.class)
    public void handleException(UsuarioNaoLocalizadoException e) {

    }

    @ResponseStatus(
            value = HttpStatus.CONFLICT,
            reason = "Usuário já cadastrado!")
    @ExceptionHandler(UsuarioJaCadastradoException.class)
    public void handleException(UsuarioJaCadastradoException e) {

    }

    @ResponseStatus(
            value = HttpStatus.UNAUTHORIZED,
            reason = "Senha atual inválida!")
    @ExceptionHandler(SenhaInvalidaException.class)
    public void handleException(SenhaInvalidaException e) {

    }

    @ResponseStatus(
            value = HttpStatus.UNAUTHORIZED,
            reason = "Usuário bloqueado!")
    @ExceptionHandler(UsuarioBloqueadoException.class)
    public void handleException(UsuarioBloqueadoException e) {

    }

    @ResponseStatus(
            value = HttpStatus.BAD_REQUEST,
            reason = "Senha deve conter no máximo 72 bytes!")
    @ExceptionHandler(SenhaForaDoPadraoException.class)
    public void handleException(SenhaForaDoPadraoException e) {

    }

    // Especifico para validação de campos
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<String, String>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
        errors.put(error.getField(), error.getDefaultMessage()));
        return errors;
    }
}
