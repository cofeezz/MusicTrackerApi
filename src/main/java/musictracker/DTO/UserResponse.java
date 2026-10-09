package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

// Nunca devolve a senha: é por isso que Response é um DTO separado de Entity.
@Schema(description = "Dados públicos de um usuário")
public record UserResponse(
        Long id,
        String username,
        String email
) {
}
