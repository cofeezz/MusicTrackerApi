package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import musictracker.Entity.Genre;

@Schema(description = "Dados de um artista")
public record ArtistResponse(
        Long id,
        String name,
        Genre genre
) {
}
