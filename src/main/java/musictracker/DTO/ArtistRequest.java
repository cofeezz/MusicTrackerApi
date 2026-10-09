package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import musictracker.Entity.Genre;

@Schema(description = "Dados para criar ou atualizar um artista")
public record ArtistRequest(

        @NotBlank(message = "O nome não pode estar em branco")
        @Schema(description = "Nome do artista/banda", example = "Phoenix")
        String name,

        @NotNull(message = "O gênero é obrigatório")
        @Schema(description = "Gênero musical", example = "INDIE")
        Genre genre
) {
}
