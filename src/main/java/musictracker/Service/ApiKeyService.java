package musictracker.Service;

import musictracker.DTO.ApiKeyCreatedResponse;
import musictracker.DTO.ApiKeyRequest;
import musictracker.DTO.ApiKeyResponse;
import musictracker.Entity.ApiKey;
import musictracker.Entity.User;
import musictracker.Exception.ResourceNotFoundException;
import musictracker.Exception.UnauthorizedException;
import musictracker.Repository.ApiKeyRepository;
import musictracker.Repository.UserRepository;
import musictracker.Security.ApiKeys;
import musictracker.Security.AuthenticatedUser;
import musictracker.Security.PasswordHasher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, UserRepository userRepository, PasswordHasher passwordHasher) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public ApiKeyCreatedResponse create(ApiKeyRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(request.username())
                .filter(found -> passwordHasher.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new UnauthorizedException("Usuário ou senha inválidos."));

        String name = request.name() == null || request.name().isBlank() ? "default" : request.name().trim();
        String rawKey = ApiKeys.generate();
        ApiKey saved = apiKeyRepository.save(new ApiKey(name, ApiKeys.hash(rawKey), ApiKeys.displayPrefix(rawKey), user));
        return new ApiKeyCreatedResponse(saved.getId(), saved.getName(), saved.getKeyPrefix(), rawKey, saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public Page<ApiKeyResponse> findMine(Pageable pageable) {
        return apiKeyRepository.findByUserId(AuthenticatedUser.requireId(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ApiKeyResponse findMineById(Long id) {
        return toResponse(getMineOrThrow(id));
    }

    public void revoke(Long id) {
        ApiKey key = getMineOrThrow(id);
        key.setRevoked(true);
        apiKeyRepository.save(key);
    }

    private ApiKey getMineOrThrow(Long id) {
        return apiKeyRepository.findByIdAndUserId(id, AuthenticatedUser.requireId())
                .orElseThrow(() -> new ResourceNotFoundException("Chave de API não encontrada com o ID: " + id));
    }

    private ApiKeyResponse toResponse(ApiKey key) {
        return new ApiKeyResponse(key.getId(), key.getName(), key.getKeyPrefix(), key.getCreatedAt(), key.isRevoked());
    }
}
