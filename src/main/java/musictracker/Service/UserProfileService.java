package musictracker.Service;

import musictracker.DTO.UserProfileRequest;
import musictracker.DTO.UserProfileResponse;
import musictracker.DTO.UserResponse;
import musictracker.Entity.User;
import musictracker.Entity.UserProfile;
import musictracker.Exception.ResourceNotFoundException;
import musictracker.Repository.UserProfileRepository;
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
        User user = userService.getEntityOrThrow(request.userId());
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
        UserProfile profile = userProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado com o ID: " + id));

        profile.setBio(request.bio());
        profile.setAvatarUrl(request.avatarUrl());
        profile.setUser(userService.getEntityOrThrow(request.userId()));

        return toResponse(userProfileRepository.save(profile));
    }

    public void delete(Long id) {
        if (!userProfileRepository.existsById(id)) {
            throw new ResourceNotFoundException("Perfil não encontrado com o ID: " + id);
        }
        userProfileRepository.deleteById(id);
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        User user = profile.getUser();
        UserResponse userResponse = new UserResponse(user.getId(), user.getUsername(), user.getEmail());
        return new UserProfileResponse(profile.getId(), profile.getBio(), profile.getAvatarUrl(), userResponse);
    }
}
