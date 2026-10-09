package musictracker.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Relacionamento One-to-One: cada usuário tem exatamente um perfil.
// Unidirecional (só o UserProfile aponta para o User) para não criar
// ciclo de serialização.
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 300, message = "A bio deve ter no máximo 300 caracteres")
    private String bio;

    private String avatarUrl;

    @NotNull(message = "O usuário é obrigatório")
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    public UserProfile() {
    }

    public UserProfile(String bio, String avatarUrl, User user) {
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "UserProfile{id=" + id + ", user=" + (user != null ? user.getUsername() : null) + "}";
    }
}
