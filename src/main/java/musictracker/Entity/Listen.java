package musictracker.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

// Liga User e Track: cada escuta pertence a um usuário e a uma faixa
// (duas relações Muitos-para-Um a partir do Listen).
@Entity
@Table(name = "listens")
public class Listen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O usuário é obrigatório")
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "A faixa é obrigatória")
    @ManyToOne
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @NotNull(message = "A data/hora da escuta é obrigatória")
    @Column(nullable = false)
    private LocalDateTime listenedAt;

    // opcional: só preenche se quiser contar minutos, não só quantidade de vezes
    @Positive(message = "A duração deve ser maior que zero")
    private Integer durationSeconds;

    public Listen() {
    }

    public Listen(User user, Track track, LocalDateTime listenedAt, Integer durationSeconds) {
        this.user = user;
        this.track = track;
        this.listenedAt = listenedAt;
        this.durationSeconds = durationSeconds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    public LocalDateTime getListenedAt() {
        return listenedAt;
    }

    public void setListenedAt(LocalDateTime listenedAt) {
        this.listenedAt = listenedAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    @Override
    public String toString() {
        return "Listen{id=" + id + ", listenedAt=" + listenedAt + "}";
    }
}
