package musictracker.Service;

import musictracker.DTO.UserProfileRequest;
import musictracker.DTO.UserProfileResponse;
import musictracker.DTO.UserResponse;
import musictracker.Entity.User;
import musictracker.Entity.UserProfile;
import musictracker.Exception.ConflictException;
import musictracker.Exception.ResourceNotFoundException;
import musictracker.Repository.UserProfileRepository;
import musictracker.Security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserService userService;

    public UserProfileService(UserProfileRepository userProfileRepository, UserService userService) {
        this.userProfileRepository = userProfileRepository;
        this.userService = userService;
    }

    public UserProfileResponse create(UserProfileRequest request) {
        AuthenticatedUser.requireSame(request.userId(), "Você só pode criar o seu próprio perfil.");
        User user = userService.getEntityOrThrow(request.userId());
        if (userProfileRepository.existsByUserId(user.getId())) {
            throw new ConflictException("O usuário já possui um perfil.");
        }
        UserProfile profile = new UserProfile(request.bio(), request.avatarUrl(), user);
        return toResponse(userProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse findById(Long id) {
        UserProfile profile = userProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado com o ID: " + id));
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse findByUserId(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado para o usuário com ID: " + userId));
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public Page<UserProfileResponse> findAll(Pageable pageable) {
        return userProfileRepository.findAll(pageable).map(this::toResponse);
    }

    public UserProfileResponse update(Long id, UserProfileRequest request) {
        AuthenticatedUser.requireSame(request.userId(), "Você só pode alterar o seu próprio perfil.");
        UserProfile profile = userProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado com o ID: " + id));
        AuthenticatedUser.requireSame(profile.getUser().getId(), "Você só pode alterar o seu próprio perfil.");

        profile.setBio(request.bio());
        profile.setAvatarUrl(request.avatarUrl());
        User user = userService.getEntityOrThrow(request.userId());
        userProfileRepository.findByUserId(user.getId())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("O usuário já possui um perfil.");
                });
        profile.setUser(user);

        return toResponse(userProfileRepository.save(profile));
    }

    public void delete(Long id) {
        UserProfile profile = userProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado com o ID: " + id));
        AuthenticatedUser.requireSame(profile.getUser().getId(), "Você só pode remover o seu próprio perfil.");
        userProfileRepository.delete(profile);
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        User user = profile.getUser();
        UserResponse userResponse = new UserResponse(user.getId(), user.getUsername(), user.getEmail());
        return new UserProfileResponse(profile.getId(), profile.getBio(), profile.getAvatarUrl(), userResponse);
    }
}
