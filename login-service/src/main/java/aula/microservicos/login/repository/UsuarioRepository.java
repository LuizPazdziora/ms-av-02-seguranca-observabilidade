package aula.microservicos.login.repository;

import org.springframework.data.repository.CrudRepository;

import aula.microservicos.login.entity.UsuarioEntity;

public interface UsuarioRepository extends CrudRepository<UsuarioEntity, String> {

    // SELECT * FROM USUARIOS ORDER BY USERNAME
    Iterable<UsuarioEntity> findAllByOrderByUsernameAsc();

}
