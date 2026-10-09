package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Perfil de um usuário (bio, avatar)")
public record UserProfileResponse(
        Long id,
        String bio,
        String avatarUrl,
        UserResponse user
) {
}
