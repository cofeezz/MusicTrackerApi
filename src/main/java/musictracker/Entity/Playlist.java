package musictracker.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.HashSet;
import java.util.Set;

// MUITOS-PARA-MUITOS: uma playlist tem várias faixas, e uma faixa pode estar
// em várias playlists. Playlist é o lado "dono" da relação (quem guarda a
// @JoinTable); Track não referencia de volta, para evitar ciclo de serialização.
@Entity
@Table(name = "playlists")
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome não pode estar em branco")
    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToMany
    @JoinTable(
            name = "playlist_track",
            joinColumns = @JoinColumn(name = "playlist_id"),
            inverseJoinColumns = @JoinColumn(name = "track_id")
    )
    private Set<Track> tracks = new HashSet<>();

    public Playlist() {
    }

    public Playlist(String name, User owner) {
        this.name = name;
        this.owner = owner;
    }

    public User getOwner() {
        return owner;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Track> getTracks() {
        return tracks;
    }

    public void setTracks(Set<Track> tracks) {
        this.tracks = tracks;
    }

    @Override
    public String toString() {
        return "Playlist{id=" + id + ", name='" + name + "', tracks=" + tracks.size() + "}";
    }
}
