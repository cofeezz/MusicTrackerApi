package musictracker.Repository;

import musictracker.Entity.Track;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<Track, Long> {

    // Consulta personalizada: busca faixas pelo nome (parcial, case-insensitive)
    Page<Track> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
