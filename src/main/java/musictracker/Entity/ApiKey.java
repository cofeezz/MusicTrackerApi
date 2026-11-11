package musictracker.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome da chave não pode estar em branco")
    @Size(max = 50, message = "O nome da chave deve ter no máximo 50 caracteres")
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 64)
    private String keyHash;

    @Column(nullable = false)
    private String keyPrefix;

    @NotNull(message = "O usuário é obrigatório")
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean revoked;

    public ApiKey() {
    }

    public ApiKey(String name, String keyHash, String keyPrefix, User user) {
        this.name = name;
        this.keyHash = keyHash;
        this.keyPrefix = keyPrefix;
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.revoked = false;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    @Override
    public String toString() {
        return "ApiKey{id=" + id + ", name='" + name + "', prefix='" + keyPrefix + "'}";
    }
}
