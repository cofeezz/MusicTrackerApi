package musictracker.Service;

import musictracker.DTO.UserRequest;
import musictracker.DTO.UserResponse;
import musictracker.Entity.User;
import musictracker.Exception.ConflictException;
import musictracker.Exception.ResourceNotFoundException;
import musictracker.Repository.ApiKeyRepository;
import musictracker.Repository.ListenRepository;
import musictracker.Repository.PlaylistRepository;
import musictracker.Repository.UserProfileRepository;
import musictracker.Repository.UserRepository;
import musictracker.Security.AuthenticatedUser;
import musictracker.Security.PasswordHasher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    private final PasswordHasher passwordHasher;
    private final ApiKeyRepository apiKeyRepository;
    private final ListenRepository listenRepository;
    private final UserProfileRepository userProfileRepository;
    private final PlaylistRepository playlistRepository;

    public UserService(UserRepository userRepository, PasswordHasher passwordHasher,
                       ApiKeyRepository apiKeyRepository, ListenRepository listenRepository,
                       UserProfileRepository userProfileRepository, PlaylistRepository playlistRepository) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.apiKeyRepository = apiKeyRepository;
        this.listenRepository = listenRepository;
        this.userProfileRepository = userProfileRepository;
        this.playlistRepository = playlistRepository;
    }

    public UserResponse create(UserRequest request) {
        ensureUnique(request, null);
        User user = new User(request.username(), request.email(), passwordHasher.hash(request.password()));
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        AuthenticatedUser.requireSame(id, "Você só pode ver a sua própria conta.");
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        return ownAccountPage(null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findByUsername(String username, Pageable pageable) {
        return ownAccountPage(username, pageable);
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
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));
        apiKeyRepository.deleteByUserId(id);
        listenRepository.deleteByUserId(id);
        userProfileRepository.deleteByUserId(id);
        playlistRepository.deleteByOwnerId(id);
        userRepository.delete(user);
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

    private Page<UserResponse> ownAccountPage(String usernameFilter, Pageable pageable) {
        List<UserResponse> own = pageable.getOffset() > 0 ? List.of()
                : userRepository.findById(AuthenticatedUser.requireId())
                        .filter(user -> usernameFilter == null
                                || user.getUsername().toLowerCase().contains(usernameFilter.toLowerCase()))
                        .map(this::toResponse)
                        .stream()
                        .toList();
        return new PageImpl<>(own, pageable, own.size());
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail());
    }
}
