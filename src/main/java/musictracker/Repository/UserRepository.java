package musictracker.Repository;

import musictracker.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    // Consulta personalizada: busca por nome de usuário (parcial, case-insensitive)
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
}
