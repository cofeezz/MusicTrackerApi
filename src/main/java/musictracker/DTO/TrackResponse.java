package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de uma faixa, com o artista embutido")
public record TrackResponse(
        Long id,
        String name,
        ArtistResponse artist
) {
}
