package aula.microservicos.login.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="LOGIN_ATTEMPTS")
public class LoginAttemptEntity {
    @Id
    @Column(name = "USERNAME")
    private String username;
    @Column(name = "FAILED_ATTEMPTS")
    private int failedAttempts;
    @Column(name = "LOCKED")
    private boolean locked;
    @Column(name = "LAST_FAILED_AT")
    private LocalDateTime lastFailedAt;
    @Column(name = "LOCKED_AT")
    private LocalDateTime lockedAt;
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    public LoginAttemptEntity() {
    }

    public LoginAttemptEntity(String username) {
        this.username = username;
        this.failedAttempts = 0;
        this.locked = false;
        this.updatedAt = LocalDateTime.now();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public LocalDateTime getLastFailedAt() {
        return lastFailedAt;
    }

    public void setLastFailedAt(LocalDateTime lastFailedAt) {
        this.lastFailedAt = lastFailedAt;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
