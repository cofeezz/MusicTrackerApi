package musictracker.Repository;

import musictracker.Entity.Playlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    // Consulta personalizada: busca playlists pelo nome (parcial, case-insensitive)
    Page<Playlist> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
