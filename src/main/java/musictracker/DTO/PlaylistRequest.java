package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para criar ou renomear uma playlist")
public record PlaylistRequest(

        @NotBlank(message = "O nome não pode estar em branco")
        @Schema(description = "Nome da playlist", example = "Foco pra estudar")
        String name
) {
}
