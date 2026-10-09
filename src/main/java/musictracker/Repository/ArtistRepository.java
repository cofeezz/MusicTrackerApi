package musictracker.Repository;

import musictracker.Entity.Artist;
import musictracker.Entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    // Consulta personalizada: artistas de um determinado gênero
    Page<Artist> findByGenre(Genre genre, Pageable pageable);
}
