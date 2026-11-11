package musictracker.Repository;

import musictracker.Entity.ApiKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    @Query("SELECT k.user.id FROM ApiKey k WHERE k.keyHash = :keyHash AND k.revoked = false")
    Optional<Long> findActiveUserIdByKeyHash(@Param("keyHash") String keyHash);

    Page<ApiKey> findByUserId(Long userId, Pageable pageable);

    Optional<ApiKey> findByIdAndUserId(Long id, Long userId);
}
