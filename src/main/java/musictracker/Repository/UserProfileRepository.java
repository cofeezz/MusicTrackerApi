package musictracker.Repository;

import musictracker.Entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    // Consulta personalizada: encontra o perfil a partir do ID do usuário
    Optional<UserProfile> findByUserId(Long userId);
}
