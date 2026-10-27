package aula.microservicos.login.repository;

import org.springframework.data.repository.CrudRepository;

import aula.microservicos.login.entity.LoginAttemptEntity;

public interface LoginAttemptRepository extends CrudRepository<LoginAttemptEntity, String> {

    // SELECT COUNT(*) FROM LOGIN_ATTEMPTS WHERE LOCKED = ?
    long countByLocked(boolean locked);

}
