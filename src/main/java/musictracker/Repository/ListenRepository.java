package musictracker.Repository;

import musictracker.DTO.TopTrackDTO;
import musictracker.Entity.Listen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ListenRepository extends JpaRepository<Listen, Long> {

    @Query("""
            SELECT new musictracker.DTO.TopTrackDTO(
                t.id, t.name, a.name, COUNT(l), COALESCE(SUM(l.durationSeconds), 0))
            FROM Listen l
            JOIN l.track t
            JOIN t.artist a
            WHERE l.user.id = :userId AND (:since IS NULL OR l.listenedAt >= :since)
            GROUP BY t.id, t.name, a.name
            ORDER BY COUNT(l) DESC, COALESCE(SUM(l.durationSeconds), 0) DESC, t.id ASC
            """)
    List<TopTrackDTO> findTopTracksSince(@Param("userId") Long userId, @Param("since") LocalDateTime since, Pageable pageable);

    // Segunda consulta personalizada, exposta em /listens/by-user/{id}
    Page<Listen> findByUserId(Long userId, Pageable pageable);
}
