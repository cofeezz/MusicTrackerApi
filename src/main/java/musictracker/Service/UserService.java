package musictracker.Service;

import musictracker.DTO.UserRequest;
import musictracker.DTO.UserResponse;
import musictracker.Entity.User;
import musictracker.Exception.ConflictException;
import musictracker.Exception.ResourceNotFoundException;
import musictracker.Repository.UserRepository;
import musictracker.Security.AuthenticatedUser;
import musictracker.Security.PasswordHasher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    private final PasswordHasher passwordHasher;

    public UserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public UserResponse create(UserRequest request) {
        ensureUnique(request, null);
        User user = new User(request.username(), request.email(), passwordHasher.hash(request.password()));
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findByUsername(String username, Pageable pageable) {
        return userRepository.findByUsernameContainingIgnoreCase(username, pageable).map(this::toResponse);
    }

    public UserResponse update(Long id, UserRequest request) {
        AuthenticatedUser.requireSame(id, "Você só pode alterar a sua própria conta.");
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));

        ensureUnique(request, id);
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordHasher.hash(request.password()));

        return toResponse(userRepository.save(user));
    }

    public void delete(Long id) {
        AuthenticatedUser.requireSame(id, "Você só pode remover a sua própria conta.");
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado com o ID: " + id);
        }
        userRepository.deleteById(id);
    }

    // Só pra outros Services resolverem um ID de usuário sem expor o Repository.
    @Transactional(readOnly = true)
    public User getEntityOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));
    }

    private void ensureUnique(UserRequest request, Long selfId) {
        userRepository.findByUsernameIgnoreCase(request.username())
                .filter(found -> !found.getId().equals(selfId))
                .ifPresent(found -> {
                    throw new ConflictException("Já existe um usuário com este nome de usuário.");
                });
        userRepository.findByEmailIgnoreCase(request.email())
                .filter(found -> !found.getId().equals(selfId))
                .ifPresent(found -> {
                    throw new ConflictException("Já existe um usuário com este e-mail.");
                });
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail());
    }
}
