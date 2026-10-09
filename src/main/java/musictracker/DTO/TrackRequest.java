package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para criar ou atualizar uma faixa")
public record TrackRequest(

        @NotBlank(message = "O nome não pode estar em branco")
        @Schema(description = "Nome da faixa", example = "1901")
        String name,

        @NotNull(message = "O ID do artista é obrigatório")
        @Schema(description = "ID do artista dono da faixa (precisa já existir)", example = "1")
        Long artistId
) {
}
