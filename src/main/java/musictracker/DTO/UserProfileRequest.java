package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criar ou atualizar o perfil de um usuário")
public record UserProfileRequest(

        @Size(max = 300, message = "A bio deve ter no máximo 300 caracteres")
        @Schema(description = "Biografia curta", example = "Dev full-stack e fã de jogos retrô")
        String bio,

        // Pattern simples: exige que, se enviado, comece com http:// ou https://.
        // Não valida se a URL realmente existe/responde (isso exigiria uma
        // chamada de rede), só o formato básico — suficiente para barrear
        // valores claramente inválidos como "123" ou "<script>".
        @Pattern(
                regexp = "^(https?://).+",
                message = "A URL do avatar deve começar com http:// ou https://"
        )
        @Schema(description = "URL do avatar (deve começar com http:// ou https://)",
                example = "https://example.com/avatar.png")
        String avatarUrl,

        @NotNull(message = "O ID do usuário é obrigatório")
        @Schema(description = "ID do usuário dono deste perfil", example = "1")
        Long userId
) {
}
