package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criar ou renomear uma playlist")
public record PlaylistRequest(

        @NotBlank(message = "O nome não pode estar em branco")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
        @Schema(description = "Nome da playlist", example = "Foco pra estudar")
        String name
) {
}
